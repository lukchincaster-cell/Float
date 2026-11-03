package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.ArrayList;

public class Main extends ApplicationAdapter {

    // สถานะของผู้เล่น
    enum State {
        MENU,      // หน้าเริ่มเกม
        SAILING,   // ขับเรือ
        CASTING,   // กำลังเหวี่ยงเบ็ด
        WAITING,   // รอปลากิน
        BITE,      // ปลากินเบ็ด! (ต้องกดทัน)
        FIGHTING,  // มินิเกมสู้กับปลา
        REELING    // ดึงปลาขึ้นมา
    }

    SpriteBatch batch;
    BitmapFont font;

    Texture background;
    Texture dock;    // ท่าเรือสำหรับขายปลา
    Texture boat;
    Texture player;
    Texture pixel;   // พิกเซลสีขาว 1x1 ใช้วาดแถบมินิเกม

    TextureRegion playerIdle;
    TextureRegion boatRegion;

    // เรือหันไปทางขวาหรือไม่ (รูปที่วาดไว้หันไปทางขวา)
    boolean facingRight = true;

    // animation ตกปลา
    Animation<TextureRegion> castAnim;
    Animation<TextureRegion> waitAnim;
    Animation<TextureRegion> reelAnim;

    // รูปปลา (ตัดขอบโปร่งใสออกแล้ว)
    TextureRegion[] fishRegions = new TextureRegion[3];

    // เก็บ texture ไว้ dispose ตอนปิดเกม
    ArrayList<Texture> extraTextures = new ArrayList<>();

    // เสียง
    Sound castSound, biteSound, catchSound, fightSound, sellSound;
    long fightSoundId = -1;
    Music bgm;

    // หน้าเริ่มเกม
    final String GAME_TITLE = "Float";   // ชื่อเกม (ถ้าไม่มี title.png จะวาดเป็นตัวอักษร)
    Texture titleImg;                    // โลโก้ title.png (ไม่บังคับ)
    GlyphLayout layout = new GlyphLayout();

    OrthographicCamera camera;
    Viewport viewport;

    // ขนาดมุมมองของเกม (ขนาดที่เห็นบนจอ)
    final float WORLD_WIDTH = 512;
    final float WORLD_HEIGHT = 288;

    // ===== ค่าที่ปรับได้ =====

    // ตำแหน่งเรือ/ตัวละคร (ตำแหน่งอยู่ในรูปแล้วเลยเป็น 0)
    float boatY = 0;

    // ความเร็วเรือ
    float speed = 120;

    // การโยกขึ้นลงของเรือ (ใส่ 0 ถ้าไม่อยากให้โยก)
    float bobHeight = 1.5f;
    float bobSpeed = 2f;

    // จำนวนเฟรมที่คาดไว้ (โค้ดนับจริงจากความกว้างไฟล์อยู่แล้ว ใช้แค่คำนวณเวลากรณีไม่มีไฟล์)
    final int CAST_FRAMES = 3;
    final int WAIT_FRAMES = 1;

    // ความเร็วของแต่ละเฟรม (วินาที)
    final float CAST_FRAME_TIME = 0.18f;
    final float WAIT_FRAME_TIME = 0.5f;
    final float REEL_FRAME_TIME = 0.15f;

    // เวลารอปลากิน (สุ่มระหว่างสองค่านี้)
    final float WAIT_MIN = 2f;
    final float WAIT_MAX = 5f;

    // เวลาที่ผู้เล่นต้องกดตอนปลากิน (วินาที)
    final float BITE_WINDOW = 1.0f;

    // ชนิดปลากับราคา (เปลี่ยนชื่อให้ตรงกับปลาที่วาดได้เลย)
    final String[] FISH_NAMES = {"Fish 1", "Fish 2", "Fish 3"};
    final int[] FISH_PRICES = {5, 12, 25};

    // ===== ค่ามินิเกมสู้ปลา =====
    // ความเร็วที่ปลาแหวกไปมา (สัดส่วนของแถบต่อวินาที) ปลาตัวแพงดิ้นแรงกว่า
    final float[] FISH_SPEED = {0.35f, 0.55f, 0.90f};
    // ขนาดโซนสีเขียว (สัดส่วนของแถบ) ปลาตัวแพงโซนเล็กกว่า = จับยากกว่า
    final float[] ZONE_SIZE = {0.32f, 0.26f, 0.20f};

    final float LIFT = 3.0f;          // แรงยกโซนขึ้นตอนกดค้าง SPACE
    final float SINK = 2.5f;          // แรงดึงโซนลงตอนปล่อย
    final float MAX_ZONE_SPEED = 1.2f;

    final float PROGRESS_START = 0.3f;   // ความคืบหน้าตอนเริ่ม
    final float PROGRESS_GAIN = 0.30f;   // เพิ่มต่อวินาทีตอนปลาอยู่ในโซน
    final float PROGRESS_LOSS = 0.20f;   // ลดต่อวินาทีตอนปลาหลุดโซน

    // โซนท่าเรือสำหรับขายปลา: ถ้ากลางเรืออยู่ซ้ายกว่าค่านี้ กด E เพื่อขาย
    // (ปรับให้ตรงกับตำแหน่งที่ท่าเรือใน dock.png สิ้นสุดจริง)
    final float DOCK_MAX_X = 150;

    // ความสูงของป้าย "E: SELL" ที่ลอยเหนือท่าเรือ (ปรับให้ตรงกับภาพ)
    final float DOCK_SIGN_Y = 120;

    // ========================

    State state = State.MENU;
    float stateTime = 0;

    float boatX = 0;
    float time = 0;

    float waitTimer = 0;
    float biteTimer = 0;

    // ปลาที่กำลังสู้อยู่
    int currentFishId = 0;

    // ตัวแปรของมินิเกม (ตำแหน่งทั้งหมดเป็นสัดส่วน 0..1 ของแถบ)
    float zoneBottom = 0;
    float zoneVel = 0;
    float fishPos = 0.5f;
    float fishTarget = 0.5f;
    float fishTimer = 0;
    float progress = 0;

    // ปลาที่จับได้ (ยังไม่ได้ขาย)
    int fishCount = 0;
    int fishValue = 0;
    int money = 0;

    // ปลาที่เพิ่งจับได้ (โชว์เหนือหัวสักพัก)
    int caughtFishId = -1;
    float caughtTimer = 0;

    // ข้อความแจ้งเตือนบนจอ
    String message = "";
    float messageTimer = 0;

    // ขอบซ้าย-ขวาของตัวเรือจริงๆ ในรูป
    int boatLeft = 0;
    int boatRight = 0;

    // ตำแหน่งของตัวละครในรูป (ใช้วางเครื่องหมาย ! และรูปปลา)
    float playerCenterX = 0;
    float playerTopY = 0;

    @Override
    public void create() {

        batch = new SpriteBatch();

        font = new BitmapFont();
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.5f);

        // พิกเซลสีขาว 1x1 สำหรับวาดสี่เหลี่ยม
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        pixel = new Texture(pm);
        pm.dispose();

        // โหลดภาพ
        background = new Texture("background.png");
        dock = new Texture("dock.png");
        boat = new Texture("boat.png");
        player = new Texture("player.png");
        playerIdle = new TextureRegion(player);
        boatRegion = new TextureRegion(boat);

        // โหลด animation (ถ้าไม่มีไฟล์จะใช้ player.png แทน)
        castAnim = loadAnimation("player_cast.png", CAST_FRAMES, CAST_FRAME_TIME, Animation.PlayMode.NORMAL);
        waitAnim = loadAnimation("player_wait.png", WAIT_FRAMES, WAIT_FRAME_TIME, Animation.PlayMode.LOOP);

        // ท่าดึงปลาขึ้น = เล่นท่าเหวี่ยงเบ็ดย้อนกลับ
        reelAnim = loadAnimation("player_cast.png", CAST_FRAMES, REEL_FRAME_TIME, Animation.PlayMode.REVERSED);

        // โหลดรูปปลา
        for (int i = 0; i < 3; i++) {
            fishRegions[i] = loadCroppedRegion("fish" + (i + 1) + ".png");
        }

        // โหลดโลโก้หน้าเริ่มเกม (ถ้ามีไฟล์ title.png)
        if (Gdx.files.internal("title.png").exists()) {
            titleImg = new Texture("title.png");
            extraTextures.add(titleImg);
        }

        // โหลดเสียง (วางไฟล์ใน assets: cast, bite, catch, fight, sell, bgm)
        castSound = loadSound("cast");
        biteSound = loadSound("bite");
        catchSound = loadSound("catch");
        fightSound = loadSound("fight");
        sellSound = loadSound("sell");

        FileHandle bgmFile = findAudio("bgm");
        if (bgmFile != null) {
            bgm = Gdx.audio.newMusic(bgmFile);
            bgm.setLooping(true);
            bgm.setVolume(0.2f);
            bgm.play();
        }

        // หาขอบเขตของเรือและตัวละครจากพิกเซลจริง
        int[] boatBounds = getOpaqueBounds("boat.png");
        if (boatBounds != null) {
            boatLeft = boatBounds[0];
            boatRight = boatBounds[2] + 1;
        } else {
            boatLeft = 0;
            boatRight = boat.getWidth();
        }

        int[] playerBounds = getOpaqueBounds("player.png");
        if (playerBounds != null) {
            playerCenterX = (playerBounds[0] + playerBounds[2] + 1) / 2f;
            // แกน Y ของรูปนับจากบน แต่ใน libGDX นับจากล่าง
            playerTopY = player.getHeight() - playerBounds[1];
        } else {
            playerCenterX = player.getWidth() / 2f;
            playerTopY = player.getHeight();
        }

        // เริ่มเกมให้เรืออยู่ทางซ้าย (ตรงท่าเรือ)
        float boatCenter = (boatLeft + boatRight) / 2f;
        boatX = DOCK_MAX_X / 2f - boatCenter;
        boatX = clampBoat(boatX);

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        camera.position.set(WORLD_WIDTH / 2, WORLD_HEIGHT / 2, 0);
        camera.update();
    }

    // หาไฟล์เสียงจากชื่อ โดยลองนามสกุล .wav .ogg .mp3 ตามลำดับ (ไม่เจอจะได้ null)
    private FileHandle findAudio(String name) {
        String[] exts = {".wav", ".ogg", ".mp3"};
        for (String ext : exts) {
            FileHandle f = Gdx.files.internal(name + ext);
            if (f.exists()) return f;
        }
        return null;
    }

    private Sound loadSound(String name) {
        FileHandle f = findAudio(name);
        if (f == null) return null;
        return Gdx.audio.newSound(f);
    }

    private void play(Sound s) {
        if (s != null) s.play(0.8f);   // 0.0 - 1.0 ความดัง
    }

    // โหลด spritesheet แนวนอนเป็น animation
    private Animation<TextureRegion> loadAnimation(String file, int frames, float frameTime, Animation.PlayMode mode) {

        // ถ้ายังไม่มีไฟล์ ใช้ภาพ player เฟรมเดียวแทน
        if (!Gdx.files.internal(file).exists()) {
            TextureRegion[] one = { playerIdle };
            Animation<TextureRegion> fallback = new Animation<>(frames * frameTime, one);
            fallback.setPlayMode(mode);
            return fallback;
        }

        Texture sheet = new Texture(file);
        extraTextures.add(sheet);

        // แต่ละเฟรมกว้างเท่ารูป player.png (canvas เท่าฉาก) แล้วนับจำนวนเฟรมจากความกว้างของไฟล์
        int frameW = player.getWidth();
        int frameH = sheet.getHeight();
        int realFrames = Math.max(1, sheet.getWidth() / frameW);

        Gdx.app.log("Anim", file + " size=" + sheet.getWidth() + "x" + sheet.getHeight()
            + " frames=" + realFrames);

        TextureRegion[][] split = TextureRegion.split(sheet, frameW, frameH);
        TextureRegion[] keyFrames = new TextureRegion[realFrames];
        for (int i = 0; i < realFrames; i++) {
            keyFrames[i] = split[0][i];
        }

        Animation<TextureRegion> anim = new Animation<>(frameTime, keyFrames);
        anim.setPlayMode(mode);
        return anim;
    }

    // โหลดรูปแล้วตัดขอบโปร่งใสออก (ใช้กับรูปปลา)
    private TextureRegion loadCroppedRegion(String file) {

        Texture tex = new Texture(file);
        extraTextures.add(tex);

        int[] b = getOpaqueBounds(file);
        if (b == null) {
            return new TextureRegion(tex);
        }

        return new TextureRegion(tex, b[0], b[1], b[2] - b[0] + 1, b[3] - b[1] + 1);
    }

    // หากรอบสี่เหลี่ยมของพิกเซลที่ไม่โปร่งใส: {minX, minY, maxX, maxY} (Y นับจากบน)
    private int[] getOpaqueBounds(String file) {

        Pixmap pixmap = new Pixmap(Gdx.files.internal(file));

        int minX = pixmap.getWidth();
        int minY = pixmap.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int x = 0; x < pixmap.getWidth(); x++) {
            for (int y = 0; y < pixmap.getHeight(); y++) {
                int alpha = pixmap.getPixel(x, y) & 0xff;
                if (alpha > 0) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        pixmap.dispose();

        if (maxX < 0) {
            return null;
        }

        return new int[] { minX, minY, maxX, maxY };
    }

    private float clampBoat(float x) {
        float minX = -boatLeft;
        float maxX = background.getWidth() - boatRight;
        return MathUtils.clamp(x, minX, maxX);
    }

    // กลางตัวเรือในพิกัดโลก
    private float boatCenterX() {
        return boatX + (boatLeft + boatRight) / 2f;
    }

    private boolean atDock() {
        return boatCenterX() < DOCK_MAX_X;
    }

    // เปลี่ยนสถานะ และรีเซ็ตเวลาของ animation
    private void setState(State newState) {

        // ออกจากมินิเกม (ชนะหรือแพ้) หยุดเสียงสู้ปลา
        if (state == State.FIGHTING && newState != State.FIGHTING) {
            if (fightSound != null && fightSoundId != -1) {
                fightSound.stop(fightSoundId);
                fightSoundId = -1;
            }
        }

        state = newState;
        stateTime = 0;

        // เข้ามินิเกม เล่นเสียงวนซ้ำ
        if (newState == State.FIGHTING && fightSound != null) {
            fightSoundId = fightSound.loop(0.6f);
        }
    }

    private void showMessage(String text) {
        message = text;
        messageTimer = 2f;
    }

    // สุ่มปลา (ปลาแพงหายากกว่า)
    private int randomFish() {
        float r = MathUtils.random();
        if (r < 0.6f) return 0;
        if (r < 0.9f) return 1;
        return 2;
    }

    // เริ่มมินิเกมสู้ปลา
    private void startFight() {
        zoneBottom = 0;
        zoneVel = 0;
        fishPos = 0.5f;
        fishTarget = 0.5f;
        fishTimer = 0;
        progress = PROGRESS_START;
        setState(State.FIGHTING);
    }

    // ===== ลอจิกของเกม =====
    private void update(float delta) {

        time += delta;
        stateTime += delta;

        if (messageTimer > 0) {
            messageTimer -= delta;
        }
        if (caughtTimer > 0) {
            caughtTimer -= delta;
        }

        boolean spacePressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);

        switch (state) {

            case MENU:
                // กด SPACE หรือ ENTER เพื่อเริ่มเกม
                if (spacePressed || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                    setState(State.SAILING);
                }
                break;

            case SAILING:
                if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                    boatX -= speed * delta;
                    facingRight = false;
                }
                if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                    boatX += speed * delta;
                    facingRight = true;
                }
                boatX = clampBoat(boatX);

                // ขายปลาที่ท่าเรือ
                if (atDock() && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                    if (fishCount > 0) {
                        showMessage("Sold " + fishCount + " fish for $" + fishValue);
                        play(sellSound);
                        money += fishValue;
                        fishCount = 0;
                        fishValue = 0;
                    } else {
                        showMessage("No fish to sell");
                    }
                }

                if (spacePressed) {
                    play(castSound);
                    setState(State.CASTING);
                }
                break;

            case CASTING:
                // เหวี่ยงเบ็ดจบแล้วไปรอปลา
                if (castAnim.isAnimationFinished(stateTime)) {
                    waitTimer = MathUtils.random(WAIT_MIN, WAIT_MAX);
                    setState(State.WAITING);
                }
                break;

            case WAITING:
                waitTimer -= delta;

                if (spacePressed) {
                    // กดเร็วไป ปลาตกใจ
                    showMessage("Too early!");
                    setState(State.SAILING);
                } else if (waitTimer <= 0) {
                    // ปลากินเบ็ด: สุ่มชนิดปลาตรงนี้ เพื่อให้รู้ความยากของมินิเกม
                    currentFishId = randomFish();
                    biteTimer = BITE_WINDOW;
                    play(biteSound);
                    setState(State.BITE);
                }
                break;

            case BITE:
                biteTimer -= delta;

                if (spacePressed) {
                    startFight();
                } else if (biteTimer <= 0) {
                    showMessage("The fish got away...");
                    setState(State.SAILING);
                }
                break;

            case FIGHTING:
                updateFight(delta);
                break;

            case REELING:
                // ดึงปลาขึ้นมาจบแล้วได้ปลา
                if (reelAnim.isAnimationFinished(stateTime)) {
                    int id = currentFishId;
                    fishCount++;
                    fishValue += FISH_PRICES[id];
                    caughtFishId = id;
                    caughtTimer = 2f;
                    showMessage("Caught " + FISH_NAMES[id] + "! ($" + FISH_PRICES[id] + ")");
                    play(catchSound);
                    setState(State.SAILING);
                }
                break;
        }
    }

    // ลอจิกมินิเกมสู้ปลา: กดค้าง SPACE ให้โซนสีเขียวลอยขึ้น ปล่อยให้จมลง
    private void updateFight(float delta) {

        float zoneSize = ZONE_SIZE[currentFishId];

        // ---- โซนสีเขียว ----
        boolean holding = Gdx.input.isKeyPressed(Input.Keys.SPACE);

        if (holding) {
            zoneVel += LIFT * delta;
        } else {
            zoneVel -= SINK * delta;
        }
        zoneVel = MathUtils.clamp(zoneVel, -MAX_ZONE_SPEED, MAX_ZONE_SPEED);

        zoneBottom += zoneVel * delta;

        float maxBottom = 1f - zoneSize;
        if (zoneBottom < 0) {
            zoneBottom = 0;
            zoneVel = 0;
        }
        if (zoneBottom > maxBottom) {
            zoneBottom = maxBottom;
            zoneVel = 0;
        }

        // ---- ปลา: สุ่มเป้าหมายใหม่เป็นพักๆ แล้วแหวกไปหา ----
        fishTimer -= delta;
        if (fishTimer <= 0) {
            fishTarget = MathUtils.random();
            fishTimer = MathUtils.random(0.5f, 1.3f);
        }

        float step = FISH_SPEED[currentFishId] * delta;
        fishPos += MathUtils.clamp(fishTarget - fishPos, -step, step);
        fishPos = MathUtils.clamp(fishPos, 0f, 1f);

        // ---- ความคืบหน้า ----
        boolean inside = fishPos >= zoneBottom && fishPos <= zoneBottom + zoneSize;

        if (inside) {
            progress += PROGRESS_GAIN * delta;
        } else {
            progress -= PROGRESS_LOSS * delta;
        }

        if (progress >= 1f) {
            setState(State.REELING);
        } else if (progress <= 0f) {
            showMessage("The fish got away...");
            setState(State.SAILING);
        }
    }

    // เลือกภาพตัวละครตามสถานะ
    private TextureRegion getPlayerFrame() {
        switch (state) {
            case CASTING:
                return castAnim.getKeyFrame(stateTime);
            case WAITING:
            case BITE:
            case FIGHTING:
                return waitAnim.getKeyFrame(stateTime);
            case REELING:
                return reelAnim.getKeyFrame(stateTime);
            default:
                return playerIdle;
        }
    }

    // วาดรูปที่มี canvas เท่าฉาก โดยกลับด้านรอบจุดกลางเรือ (เรือไม่เลื่อนตำแหน่งตอนหัน)
    private void drawFlippable(TextureRegion region, float y) {

        float w = region.getRegionWidth();
        float h = region.getRegionHeight();
        float c = (boatLeft + boatRight) / 2f;

        if (facingRight) {
            batch.draw(region, boatX, y, w, h);
        } else {
            // ความกว้างติดลบ = กลับซ้ายขวา
            batch.draw(region, boatX + 2 * c, y, -w, h);
        }
    }

    // วาดสี่เหลี่ยมสีทึบ
    private void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(pixel, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    // วาดข้อความแบบกำหนดสีและขนาดเอง (เสร็จแล้วคืนค่าเดิมให้ฟอนต์)
    private void drawText(String text, float x, float y, float scale, Color color) {
        float oldScale = font.getData().scaleX;
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
        font.getData().setScale(oldScale);
    }

    // วาดข้อความกึ่งกลางที่ centerX พร้อมกำหนดสีและขนาด
    private void drawCentered(String text, float centerX, float y, float scale, Color color) {
        float oldScale = font.getData().scaleX;
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.setColor(color);
        font.draw(batch, text, centerX - layout.width / 2f, y);
        font.setColor(Color.WHITE);
        font.getData().setScale(oldScale);
    }

    // หน้าเริ่มเกม
    private void drawMenu() {

        float cx = camera.position.x;

        // ม่านดำบางๆ ทับฉาก
        rect(cx - WORLD_WIDTH / 2, 0, WORLD_WIDTH, WORLD_HEIGHT, 0, 0, 0, 0.35f);

        // ชื่อเกม
        if (titleImg != null) {
            float w = titleImg.getWidth();
            float h = titleImg.getHeight();
            batch.draw(titleImg, cx - w / 2f, WORLD_HEIGHT - h - 30, w, h);
        } else {
            drawCentered(GAME_TITLE, cx, WORLD_HEIGHT - 60, 2.2f, Color.WHITE);
        }

        // ข้อความกะพริบ
        if ((int) (time * 2) % 2 == 0) {
            drawCentered("Press SPACE to start", cx, 110, 0.8f, Color.YELLOW);
        }

        // วิธีเล่น
        drawCentered("A/D: move   SPACE: cast   E: sell at the dock", cx, 28, 0.5f, Color.WHITE);
    }

    // วาดแถบมินิเกมที่ขอบขวาของจอ
    private void drawFightUI() {

        float barH = 140;
        float barW = 14;
        float barX = camera.position.x + WORLD_WIDTH / 2 - 44;
        float barY = 70;

        float zoneSize = ZONE_SIZE[currentFishId];

        // กรอบกับพื้นแถบ
        rect(barX - 2, barY - 2, barW + 4, barH + 4, 0, 0, 0, 0.8f);
        rect(barX, barY, barW, barH, 0.15f, 0.25f, 0.4f, 1f);

        // โซนสีเขียว
        rect(barX, barY + zoneBottom * barH, barW, zoneSize * barH, 0.2f, 0.9f, 0.3f, 0.75f);

        // ไอคอนปลา (ย่อให้กว้างไม่เกิน barW + 6)
        TextureRegion fish = fishRegions[currentFishId];
        float scale = (barW + 6) / fish.getRegionWidth();
        float fw = fish.getRegionWidth() * scale;
        float fh = fish.getRegionHeight() * scale;
        float fy = barY + fishPos * barH - fh / 2;
        batch.draw(fish, barX + barW / 2 - fw / 2, fy, fw, fh);

        // แถบความคืบหน้า (ข้างๆ)
        float pX = barX + barW + 6;
        rect(pX - 1, barY - 2, 8, barH + 4, 0, 0, 0, 0.8f);
        rect(pX, barY, 6, barH * progress, 1f, 0.8f, 0.2f, 1f);
    }

    @Override
    public void render() {

        float delta = Gdx.graphics.getDeltaTime();

        update(delta);

        // เรือโยกขึ้นลงเบาๆ
        float bob = MathUtils.sin(time * bobSpeed) * bobHeight;

        // กล้องติดตามเรือ
        float camX;

        if (background.getWidth() > WORLD_WIDTH) {
            camX = MathUtils.clamp(
                boatCenterX(),
                WORLD_WIDTH / 2,
                background.getWidth() - WORLD_WIDTH / 2
            );
        } else {
            camX = background.getWidth() / 2f;
        }

        camera.position.set(camX, WORLD_HEIGHT / 2, 0);
        camera.update();

        // ล้างหน้าจอ
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();

        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // พื้นหลัง
        batch.draw(background, 0, 0);

        // ท่าเรือ (วาดทับพื้นหลัง ก่อนเรือ)
        batch.draw(dock, 0, 0);

        // เรือ และตัวละคร (ทุกรูปมีขนาด canvas เท่ากับฉาก เลยวางที่ตำแหน่งเดียวกัน)
        drawFlippable(boatRegion, boatY + bob);
        drawFlippable(getPlayerFrame(), boatY + bob);

        // หน้าเริ่มเกม: วาดทับฉาก แล้วจบการวาดแค่นี้ (ไม่ต้องแสดง HUD)
        if (state == State.MENU) {
            drawMenu();
            batch.end();
            return;
        }

        // ตำแหน่งเหนือหัวตัวละคร (ถ้าเรือหันซ้าย ตำแหน่งก็กลับด้านด้วย)
        float boatMid = (boatLeft + boatRight) / 2f;
        float headX = facingRight
            ? boatX + playerCenterX
            : boatX + 2 * boatMid - playerCenterX;
        float headY = boatY + bob + playerTopY;

        // เครื่องหมาย ! ตอนปลากินเบ็ด
        if (state == State.BITE) {
            drawText("!", headX, headY + 16, 0.9f, Color.YELLOW);
        }

        // โชว์ปลาที่เพิ่งจับได้เหนือหัว
        if (caughtTimer > 0 && caughtFishId >= 0) {
            TextureRegion fish = fishRegions[caughtFishId];
            float fw = fish.getRegionWidth();
            batch.draw(fish, headX - fw / 2, headY + 8);
        }

        // ป้ายบอกตอนอยู่ที่ท่าเรือ
        if (state == State.SAILING && atDock()) {
            drawText("E: SELL", DOCK_MAX_X / 2f - 20, DOCK_SIGN_Y, 0.75f, Color.YELLOW);
        }

        // มินิเกมสู้ปลา
        if (state == State.FIGHTING) {
            drawFightUI();
        }

        // ===== HUD =====
        float hudLeft = camera.position.x - WORLD_WIDTH / 2 + 6;
        float hudTop = WORLD_HEIGHT - 6;

        font.draw(batch, "Money: $" + money, hudLeft, hudTop);
        font.draw(batch, "Fish: " + fishCount + " ($" + fishValue + ")", hudLeft, hudTop - 14);

        if (state == State.SAILING) {
            if (atDock()) {
                font.draw(batch, "E: sell fish   SPACE: cast", hudLeft, 14);
            } else {
                font.draw(batch, "A/D: move   SPACE: cast", hudLeft, 14);
            }
        } else if (state == State.BITE) {
            font.draw(batch, "SPACE NOW!", hudLeft, 14);
        } else if (state == State.WAITING) {
            font.draw(batch, "Waiting for a bite...", hudLeft, 14);
        } else if (state == State.FIGHTING) {
            font.draw(batch, "Hold SPACE: keep the fish in the green zone!", hudLeft, 14);
        }

        if (messageTimer > 0) {
            font.draw(batch, message, camera.position.x - 40, hudTop - 40);
        }

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {

        batch.dispose();
        font.dispose();

        background.dispose();
        dock.dispose();
        boat.dispose();
        player.dispose();
        pixel.dispose();

        if (castSound != null) castSound.dispose();
        if (biteSound != null) biteSound.dispose();
        if (catchSound != null) catchSound.dispose();
        if (fightSound != null) fightSound.dispose();
        if (sellSound != null) sellSound.dispose();
        if (bgm != null) bgm.dispose();

        for (Texture t : extraTextures) {
            t.dispose();
        }
    }
}
