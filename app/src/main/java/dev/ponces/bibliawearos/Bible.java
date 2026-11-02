package dev.ponces.bibliawearos;

import android.content.res.AssetManager;
import android.util.LruCache;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class Bible {
    final List<String> names = new ArrayList<>();
    final List<int[]> counts = new ArrayList<>();
    private final AssetManager assets;
    private final LruCache<String, JSONArray> chapters = new LruCache<>(4);
    private static final String[] DAILY = {
        "19:23:1", "43:3:16", "50:4:13", "19:119:105", "20:3:5", "40:11:28",
        "19:46:1", "23:41:10", "45:8:28", "46:13:4", "19:121:2", "43:14:27",
        "6:1:9", "19:27:1", "50:4:6", "20:3:6", "40:6:33", "19:37:5",
        "24:29:11", "45:12:12", "58:11:1", "62:4:19", "19:34:8", "43:8:12",
        "48:5:22", "19:100:5", "23:40:31", "40:5:9", "52:5:16", "19:118:24",
        "43:15:12", "66:21:4"
    };

    Bible(AssetManager assets) throws Exception {
        this.assets = assets;
        JSONArray books = new JSONArray(read("books.json"));
        for (int i = 0; i < books.length(); i++) {
            JSONObject book = books.getJSONObject(i);
            names.add(book.getString("name"));
            JSONArray verses = book.getJSONArray("verses");
            int[] sizes = new int[verses.length()];
            for (int j = 0; j < sizes.length; j++) sizes[j] = verses.getInt(j);
            counts.add(sizes);
        }
    }

    String label(Reference ref) {
        return names.get(ref.book - 1) + " " + ref.chapter + ":" + ref.verse;
    }

    String text(Reference ref) throws Exception {
        String key = ref.book + "/" + ref.chapter;
        JSONArray chapter = chapters.get(key);
        if (chapter == null) {
            chapter = new JSONArray(read("chapters/" + key + ".json"));
            chapters.put(key, chapter);
        }
        return chapter.getString(ref.verse - 1);
    }

    Reference daily() {
        return Reference.parse(DAILY[Math.floorMod(LocalDate.now().toEpochDay(), DAILY.length)]);
    }

    String licence() throws IOException {
        return read("THIRD_PARTY_NOTICES.md") + "\n\n" + read("LICENSE");
    }

    private String read(String path) throws IOException {
        try (InputStream input = assets.open(path)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
