package pl.maciekkov.blockdodge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        setContentView(new GameView(this));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUi();
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private static class GameView extends View {
        private static final int BG = Color.rgb(10, 14, 22);
        private static final int PLAYER = Color.rgb(48, 171, 255);
        private static final int BLOCK = Color.rgb(239, 243, 248);
        private static final int TEXT = Color.rgb(244, 247, 250);
        private static final int MUTED = Color.rgb(145, 157, 174);
        private static final int DANGER = Color.rgb(255, 82, 82);

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Random random = new Random();
        private final ArrayList<FallingBlock> blocks = new ArrayList<>();
        private final SharedPreferences prefs;

        private final RectF player = new RectF();

        private float density;
        private float playerW;
        private float playerH;
        private float playerY;
        private long lastFrameNs;
        private long lastSpawnMs;

        private boolean running = false;
        private boolean gameOver = false;
        private float score = 0f;
        private int best = 0;
        private int level = 1;

        GameView(Context context) {
            super(context);
            density = getResources().getDisplayMetrics().density;
            prefs = context.getSharedPreferences("block_dodge", Context.MODE_PRIVATE);
            best = prefs.getInt("best", 0);

            paint.setStyle(Paint.Style.FILL);
            textPaint.setColor(TEXT);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            setBackgroundColor(BG);
            setFocusable(true);
        }

        private float dp(float value) {
            return value * density;
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            playerW = Math.min(dp(92), w * 0.22f);
            playerH = dp(24);
            playerY = h - dp(78);
            player.set((w - playerW) / 2f, playerY,
                    (w + playerW) / 2f, playerY + playerH);
        }

        private void startNewGame(float touchX) {
            blocks.clear();
            score = 0f;
            level = 1;
            running = true;
            gameOver = false;
            lastFrameNs = System.nanoTime();
            lastSpawnMs = android.os.SystemClock.uptimeMillis();
            movePlayer(touchX);
            invalidate();
        }

        private void movePlayer(float x) {
            float left = x - playerW / 2f;
            left = Math.max(0f, Math.min(getWidth() - playerW, left));
            player.set(left, playerY, left + playerW, playerY + playerH);
        }

        private void spawnBlock() {
            float minW = dp(34);
            float maxW = Math.min(dp(104), getWidth() * 0.28f);
            float width = minW + random.nextFloat() * Math.max(1f, maxW - minW);
            float height = dp(18) + random.nextFloat() * dp(34);
            float x = random.nextFloat() * Math.max(1f, getWidth() - width);
            float speed = dp(190 + level * 19 + random.nextInt(75));
            blocks.add(new FallingBlock(new RectF(x, -height - dp(6), x + width, -dp(6)), speed));
        }

        private void update(float dt) {
            score += dt * 12f;
            level = 1 + ((int) score / 90);

            long nowMs = android.os.SystemClock.uptimeMillis();
            long spawnEvery = Math.max(260L, 820L - level * 24L);
            if (nowMs - lastSpawnMs >= spawnEvery) {
                spawnBlock();
                if (level >= 7 && random.nextFloat() < 0.15f) {
                    spawnBlock();
                }
                lastSpawnMs = nowMs;
            }

            Iterator<FallingBlock> it = blocks.iterator();
            while (it.hasNext()) {
                FallingBlock block = it.next();
                block.rect.offset(0f, block.speed * dt);

                if (RectF.intersects(player, block.rect)) {
                    finishGame();
                    return;
                }

                if (block.rect.top > getHeight() + dp(20)) {
                    it.remove();
                    score += 3f;
                }
            }
        }

        private void finishGame() {
            running = false;
            gameOver = true;
            int finalScore = (int) score;
            if (finalScore > best) {
                best = finalScore;
                prefs.edit().putInt("best", best).apply();
            }
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            paint.setColor(BG);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

            drawGrid(canvas);

            if (running) {
                long now = System.nanoTime();
                float dt = Math.min(0.035f, Math.max(0f, (now - lastFrameNs) / 1_000_000_000f));
                lastFrameNs = now;
                update(dt);
            }

            paint.setColor(PLAYER);
            canvas.drawRoundRect(player, dp(7), dp(7), paint);

            paint.setColor(BLOCK);
            for (FallingBlock block : blocks) {
                canvas.drawRoundRect(block.rect, dp(5), dp(5), paint);
            }

            drawHud(canvas);

            if (!running) {
                drawOverlay(canvas);
            }

            if (running) {
                postInvalidateOnAnimation();
            }
        }

        private void drawGrid(Canvas canvas) {
            paint.setColor(Color.rgb(23, 31, 45));
            paint.setStrokeWidth(dp(1));
            float gap = dp(40);
            for (float y = gap; y < getHeight(); y += gap) {
                canvas.drawLine(0, y, getWidth(), y, paint);
            }
        }

        private void drawHud(Canvas canvas) {
            textPaint.setColor(TEXT);
            textPaint.setTextSize(dp(22));
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText("Wynik " + (int) score, dp(18), dp(38), textPaint);

            textPaint.setTextSize(dp(14));
            textPaint.setColor(MUTED);
            canvas.drawText("Rekord " + best + "   •   Poziom " + level, dp(18), dp(61), textPaint);
        }

        private void drawOverlay(Canvas canvas) {
            float cx = getWidth() / 2f;
            float cy = getHeight() * 0.43f;

            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(Typeface.DEFAULT_BOLD);
            textPaint.setTextSize(dp(30));
            textPaint.setColor(gameOver ? DANGER : TEXT);
            canvas.drawText(gameOver ? "KONIEC GRY" : "UNIKAJ KLOCKÓW", cx, cy, textPaint);

            textPaint.setTextSize(dp(17));
            textPaint.setColor(TEXT);
            if (gameOver) {
                canvas.drawText("Wynik: " + (int) score, cx, cy + dp(38), textPaint);
                textPaint.setTextSize(dp(15));
                textPaint.setColor(MUTED);
                canvas.drawText("Dotknij ekranu, aby zagrać ponownie", cx, cy + dp(72), textPaint);
            } else {
                canvas.drawText("Przesuwaj dolny klocek palcem", cx, cy + dp(38), textPaint);
                textPaint.setTextSize(dp(15));
                textPaint.setColor(MUTED);
                canvas.drawText("Dotknij ekranu, aby zacząć", cx, cy + dp(72), textPaint);
            }

            textPaint.setTextAlign(Paint.Align.LEFT);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();

            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (!running) {
                    startNewGame(x);
                } else {
                    movePlayer(x);
                }
                return true;
            }

            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                if (running) {
                    movePlayer(x);
                    invalidate();
                }
                return true;
            }

            return true;
        }

        private static class FallingBlock {
            final RectF rect;
            final float speed;

            FallingBlock(RectF rect, float speed) {
                this.rect = rect;
                this.speed = speed;
            }
        }
    }
}
