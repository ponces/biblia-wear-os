package dev.ponces.bibliawearos;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.VelocityTracker;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int GOLD = Color.rgb(218, 193, 138);
    private static final int WHITE = Color.rgb(242, 240, 233);
    private static final int MUTED = Color.rgb(174, 176, 166);
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private Set<String> favourites;
    private Bible bible;
    private LinearLayout column;
    private ScrollView scroll;
    private String route = "home";
    private Bundle scrollPositions = new Bundle();
    private BackSwipe backSwipe;
    private VelocityTracker swipeVelocity;
    private boolean settlingSwipe;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences("bible", MODE_PRIVATE);
        favourites = new HashSet<>(prefs.getStringSet("favourites", new HashSet<>()));
        if (state != null) {
            route = state.getString("route", "home");
            ArrayList<String> previous = state.getStringArrayList("history");
            if (previous != null) history.addAll(previous);
            Bundle positions = state.getBundle("scrollPositions");
            if (positions != null) scrollPositions = positions;
        }
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this::back);
        }
        page("Bíblia Wear OS");
        text("A preparar a leitura…", 15, MUTED);
        loader.execute(() -> {
            try {
                Bible loaded = new Bible(getAssets());
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    bible = loaded;
                    favourites.removeIf(value -> validReference(value) == null);
                    show();
                    restoreScroll();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    page("Não foi possível abrir");
                    text("Fecha e volta a abrir a aplicação. Se o problema continuar, reinstala-a.", 15, WHITE);
                });
            }
        });
    }

    private void page(String title) {
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(true);
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        int width = getResources().getDisplayMetrics().widthPixels;
        boolean round = getResources().getConfiguration().isScreenRound();
        int side = round ? Math.round(width * .14f) : dp(16);
        column.setPadding(side, dp(round ? 30 : 18), side, dp(round ? 40 : 24));
        scroll.addView(column);
        setContentView(scroll);
        scroll.setFocusableInTouchMode(true);
        scroll.requestFocus();
        float factor = ViewConfiguration.get(this).getScaledVerticalScrollFactor();
        scroll.setOnGenericMotionListener((view, event) -> {
            if (settlingSwipe) return true;
            if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                float rotation = event.getAxisValue(MotionEvent.AXIS_SCROLL);
                if (rotation != 0) {
                    scroll.smoothScrollBy(0, Math.round(-rotation * factor));
                    return true;
                }
            }
            return false;
        });
        TextView clock = text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), 12, MUTED);
        clock.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        TextView heading = text(title, 21, GOLD);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setAccessibilityHeading(true);
    }

    private TextView text(CharSequence value, int size, int colour) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(colour);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(5), 0, dp(7));
        view.setLineSpacing(dp(3), 1);
        column.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        view.setTextSize(14);
        view.setTextColor(primary ? Color.BLACK : WHITE);
        view.setMinHeight(dp(48));
        view.setMinimumHeight(dp(48));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? GOLD : Color.rgb(30, 33, 28));
        background.setCornerRadius(dp(28));
        view.setBackground(background);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(-1, -2);
        layout.setMargins(0, dp(4), 0, dp(4));
        column.addView(view, layout);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    private void open(String next) {
        slideToPage(1, () -> {
            scrollPositions.putInt(route, scroll.getScrollY());
            if (history.size() >= 16) history.removeFirst();
            history.addLast(route);
            route = next;
            show();
        });
    }

    private void show() {
        if (bible == null) return;
        try {
            if (route.equals("home")) home();
            else if (route.equals("books")) books();
            else if (route.equals("favourites")) favourites();
            else if (route.equals("settings")) settings();
            else if (route.equals("licence")) licence();
            else if (route.startsWith("chapters/")) chapters(Integer.parseInt(route.substring(9)));
            else if (route.startsWith("chapter/")) chapter(Reference.parse(route.substring(8)));
            else if (route.startsWith("verses/")) verses(Reference.parse(route.substring(7)));
            else if (route.startsWith("read/")) reader(Reference.parse(route.substring(5)));
            else throw new IllegalArgumentException();
        } catch (Exception error) {
            page("Leitura indisponível");
            text("Não foi possível abrir esta passagem.", 15, WHITE);
            button("Voltar ao início", true, this::goHome);
        }
    }

    private void home() {
        page("Bíblia Wear OS");
        text("Um momento de paz.", 14, MUTED);
        Reference daily = bible.daily();
        button("Versículo do dia\n" + bible.label(daily), true, () -> open("read/" + daily));
        Reference last = validReference(prefs.getString("last", ""));
        if (last != null) {
            boolean chapterMode = prefs.getBoolean("lastChapter", false);
            String label = chapterMode ? bible.names.get(last.book - 1) + " " + last.chapter : bible.label(last);
            button("Continuar\n" + label, false, () -> open((chapterMode ? "chapter/" : "read/") + last));
        }
        button("Ler a Bíblia", false, () -> open("books"));
        button("Favoritos · " + favourites.size(), false, () -> open("favourites"));
        button("Definições", false, () -> open("settings"));
        text("Sempre contigo. Sem Internet.", 12, MUTED);
    }

    private void books() {
        page("Livros");
        for (int i = 0; i < bible.names.size(); i++) {
            if (i == 0 || i == 39) text(i == 0 ? "Antigo Testamento" : "Novo Testamento", 14, MUTED);
            int number = i + 1;
            button(bible.names.get(i), false, () -> open("chapters/" + number));
        }
        backButton();
    }

    private void chapters(int book) {
        page(bible.names.get(book - 1));
        text("Escolhe o capítulo", 14, MUTED);
        for (int i = 1; i <= bible.counts.get(book - 1).length; i++) {
            Reference ref = new Reference(book, i, 1);
            button("Capítulo " + i, false, () -> open("chapter/" + ref));
        }
        backButton();
    }

    private void chapter(Reference ref) throws Exception {
        if (!ref.isValid(bible.counts)) throw new IllegalArgumentException();
        prefs.edit().putString("last", ref.toString()).putBoolean("lastChapter", true).apply();
        page(bible.names.get(ref.book - 1) + " " + ref.chapter);
        int count = bible.counts.get(ref.book - 1)[ref.chapter - 1];
        text(count + " versículos", 12, MUTED);
        text("Toca num versículo para ver opções.", 12, MUTED);
        int size = prefs.getInt("font", 18);
        for (int number = 1; number <= count; number++) {
            Reference verse = new Reference(ref.book, ref.chapter, number);
            String passage = bible.text(verse);
            String marker = Integer.toString(number);
            SpannableString marked = new SpannableString(marker + "  " + passage);
            int flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
            marked.setSpan(new SuperscriptSpan(), 0, marker.length(), flags);
            marked.setSpan(new RelativeSizeSpan(.75f), 0, marker.length(), flags);
            marked.setSpan(new ForegroundColorSpan(GOLD), 0, marker.length(), flags);
            marked.setSpan(new StyleSpan(Typeface.BOLD), 0, marker.length(), flags);
            TextView line = text(marked, size, WHITE);
            line.setGravity(Gravity.START);
            line.setMinHeight(dp(48));
            line.setContentDescription(bible.label(verse) + ". " + passage);
            line.setOnClickListener(view -> open("read/" + verse));
        }
        Reference next = ref.moveChapter(1, bible.counts);
        Reference previous = ref.moveChapter(-1, bible.counts);
        if (next != null) button("Capítulo seguinte ›", true, () -> replaceChapter(next, 1));
        if (previous != null) button("‹ Capítulo anterior", false, () -> replaceChapter(previous, -1));
        button("Capítulos", false, () -> open("chapters/" + ref.book));
        backButton();
    }

    private void replaceChapter(Reference ref, int direction) {
        slideToPage(direction, () -> {
            route = "chapter/" + ref;
            show();
        });
    }

    private void verses(Reference ref) {
        page(bible.names.get(ref.book - 1) + " " + ref.chapter);
        text("Escolhe o versículo", 14, MUTED);
        for (int i = 1; i <= bible.counts.get(ref.book - 1)[ref.chapter - 1]; i++) {
            Reference selected = new Reference(ref.book, ref.chapter, i);
            button("Versículo " + i, false, () -> {
                slideToPage(1, () -> {
                    // Replace the verse picker, keeping the original reader's parent.
                    if (!history.isEmpty()) history.removeLast();
                    route = "read/" + selected;
                    show();
                });
            });
        }
        backButton();
    }

    private void reader(Reference ref) throws Exception {
        if (!ref.isValid(bible.counts)) throw new IllegalArgumentException();
        String passage = bible.text(ref);
        prefs.edit().putString("last", ref.toString()).putBoolean("lastChapter", false).apply();
        page(bible.label(ref));
        int size = prefs.getInt("font", 18);
        text(passage, size, WHITE);
        text("Versículo " + ref.verse + " de " + bible.counts.get(ref.book - 1)[ref.chapter - 1], 12, MUTED);
        button(favourites.contains(ref.toString()) ? "★ Guardado" : "☆ Guardar favorito", false, () -> {
            int position = scroll.getScrollY();
            boolean removed = !favourites.add(ref.toString());
            if (removed) favourites.remove(ref.toString());
            prefs.edit().putStringSet("favourites", new HashSet<>(favourites)).apply();
            Toast.makeText(this, removed ? "Removido dos favoritos" : "Guardado nos favoritos", Toast.LENGTH_SHORT).show();
            show();
            scroll.post(() -> scroll.scrollTo(0, position));
        });
        Reference next = ref.move(1, bible.counts);
        Reference previous = ref.move(-1, bible.counts);
        if (next != null) button("Seguinte ›", true, () -> replaceReader(next, 1));
        if (previous != null) button("‹ Anterior", false, () -> replaceReader(previous, -1));
        button("Escolher versículo", false, () -> open("verses/" + ref));
        button("Capítulos", false, () -> open("chapters/" + ref.book));
        backButton();
    }

    private void replaceReader(Reference ref, int direction) {
        slideToPage(direction, () -> {
            route = "read/" + ref;
            show();
        });
    }

    private void favourites() {
        page("Favoritos");
        if (favourites.isEmpty()) text("Guarda um versículo durante a leitura. Encontrá-lo-ás aqui.", 16, MUTED);
        ArrayList<Reference> refs = new ArrayList<>();
        for (String saved : favourites) {
            Reference ref = validReference(saved);
            if (ref != null) refs.add(ref);
        }
        refs.sort(Comparator.comparingInt((Reference r) -> r.book)
                .thenComparingInt(r -> r.chapter).thenComparingInt(r -> r.verse));
        for (Reference ref : refs) button(bible.label(ref), false, () -> open("read/" + ref));
        backButton();
    }

    private void settings() {
        page("Definições");
        text("Tamanho da letra", 15, WHITE);
        int selected = prefs.getInt("font", 18);
        int[] sizes = {16, 18, 22};
        String[] labels = {"Pequena", "Normal", "Grande"};
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            button(labels[i] + (selected == size ? " ✓" : ""), selected == size, () -> {
                prefs.edit().putInt("font", size).apply();
                show();
            });
        }
        text("Sobre esta Bíblia", 17, GOLD);
        text("Interface em português de Portugal.\n\nTexto: Almeida Revista e Corrigida (Portugal), Sociedade Bíblica de Portugal.\n\n66 livros, sem os deuterocanónicos.\n\nTradução de João Ferreira de Almeida, Edição Revista e Corrigida. Copyright © 2001 Sociedade Bíblica de Portugal.\n\nFonte dos dados: mrk214/snapshots. O texto bíblico não está abrangido pela licença GPL do código da aplicação.\n\nSem anúncios, conta, permissões de rede ou recolha de dados.", 13, MUTED);
        button("Licença e créditos", false, () -> open("licence"));
        backButton();
    }

    private void licence() throws Exception {
        page("Licença e créditos");
        TextView notice = text(bible.licence(), 12, MUTED);
        notice.setGravity(Gravity.START);
        backButton();
    }

    private Reference validReference(String saved) {
        try {
            Reference ref = Reference.parse(saved);
            return ref.isValid(bible.counts) ? ref : null;
        } catch (RuntimeException error) { return null; }
    }

    private void backButton() { button("Voltar", false, this::back); }
    private void restoreScroll() {
        int position = scrollPositions.getInt(route, 0);
        ScrollView target = scroll;
        target.post(() -> target.scrollTo(0, position));
    }
    private void goHome() { history.clear(); route = "home"; show(); }
    private void back() {
        if (settlingSwipe) return;
        resetSwipe();
        if (bible == null) { finish(); return; }
        if (!history.isEmpty()) { route = history.removeLast(); show(); restoreScroll(); }
        else if (!route.equals("home")) goHome();
        else finish();
    }

    // API 30–32 fallback only. API 33+ uses the dispatcher registered in onCreate.
    @SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { back(); }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (settlingSwipe) return true;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            resetSwipe();
            backSwipe = new BackSwipe(event.getX(), event.getY(),
                    getResources().getDisplayMetrics().widthPixels,
                    ViewConfiguration.get(this).getScaledTouchSlop());
            swipeVelocity = VelocityTracker.obtain();
        }
        if (swipeVelocity != null) swipeVelocity.addMovement(event);
        if (backSwipe != null && action == MotionEvent.ACTION_MOVE) {
            boolean alreadyDragging = backSwipe.isDragging();
            if (backSwipe.move(event.getX(), event.getY())) {
                if (!alreadyDragging) {
                    // Cancel a pressed verse/button or the vertical ScrollView exactly once.
                    MotionEvent cancel = MotionEvent.obtain(event);
                    cancel.setAction(MotionEvent.ACTION_CANCEL);
                    super.dispatchTouchEvent(cancel);
                    cancel.recycle();
                }
                scroll.setTranslationX(backSwipe.distance());
                return true;
            }
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN && backSwipe != null) {
            // A second finger must never accidentally trigger navigation.
            if (backSwipe.isDragging()) {
                settleSwipe(false);
                return true;
            }
            recycleSwipeVelocity();
            backSwipe = null;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            if (backSwipe != null && backSwipe.isDragging()) {
                backSwipe.move(event.getX(), event.getY());
                float velocity = 0;
                if (swipeVelocity != null) {
                    swipeVelocity.computeCurrentVelocity(1000);
                    velocity = swipeVelocity.getXVelocity();
                }
                boolean complete = action == MotionEvent.ACTION_UP
                        && backSwipe.shouldFinish(velocity, Math.max(dp(600),
                        ViewConfiguration.get(this).getScaledMinimumFlingVelocity()));
                settleSwipe(complete);
                return true;
            }
            recycleSwipeVelocity();
            backSwipe = null;
        }
        return super.dispatchTouchEvent(event);
    }

    private void settleSwipe(boolean complete) {
        recycleSwipeVelocity();
        backSwipe = null;
        if (!ValueAnimator.areAnimatorsEnabled()) {
            resetSwipe();
            if (complete) back();
            return;
        }
        settlingSwipe = true;
        ScrollView outgoing = scroll;
        int width = getResources().getDisplayMetrics().widthPixels;
        outgoing.animate().translationX(complete ? width : 0)
                .setDuration(complete ? 180 : 160)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    settlingSwipe = false;
                    if (!complete || isFinishing() || isDestroyed()) return;
                    back();
                    if (isFinishing() || scroll == outgoing) return;
                    animatePageEntry(-1);
                }).start();
    }

    /** Positive direction advances left; negative direction returns right. */
    private void slideToPage(int direction, Runnable navigate) {
        if (settlingSwipe || isFinishing() || isDestroyed()) return;
        resetSwipe();
        if (!ValueAnimator.areAnimatorsEnabled()) {
            navigate.run();
            return;
        }
        settlingSwipe = true;
        ScrollView outgoing = scroll;
        int width = getResources().getDisplayMetrics().widthPixels;
        outgoing.animate().translationX(-direction * width).setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    settlingSwipe = false;
                    if (isFinishing() || isDestroyed()) return;
                    navigate.run();
                    if (!isFinishing() && scroll != outgoing) animatePageEntry(direction);
                }).start();
    }

    private void animatePageEntry(int direction) {
        ScrollView incoming = scroll;
        int width = getResources().getDisplayMetrics().widthPixels;
        incoming.setTranslationX(direction * width * .18f);
        incoming.setAlpha(.6f);
        settlingSwipe = true;
        incoming.animate().translationX(0).alpha(1).setDuration(160)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> settlingSwipe = false).start();
    }

    private void recycleSwipeVelocity() {
        if (swipeVelocity != null) {
            swipeVelocity.recycle();
            swipeVelocity = null;
        }
    }

    private void resetSwipe() {
        recycleSwipeVelocity();
        backSwipe = null;
        settlingSwipe = false;
        if (scroll != null) {
            scroll.animate().withEndAction(null).cancel();
            scroll.setTranslationX(0);
            scroll.setAlpha(1);
        }
    }

    @Override protected void onStop() {
        // Do not navigate from an animation callback after the watch leaves the app.
        resetSwipe();
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("route", route);
        state.putStringArrayList("history", new ArrayList<>(history));
        scrollPositions.putInt(route, scroll.getScrollY());
        state.putBundle("scrollPositions", scrollPositions);
    }

    @Override protected void onDestroy() {
        loader.shutdownNow();
        super.onDestroy();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
