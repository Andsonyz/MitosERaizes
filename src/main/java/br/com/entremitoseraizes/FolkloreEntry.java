package br.com.entremitoseraizes;

/** Verbete educativo disponível no Códice. */
final class FolkloreEntry {
    final String name;
    final String title;
    final String text;
    final int color;

    FolkloreEntry(String name, String title, String text, int color) {
        this.name = name;
        this.title = title;
        this.text = text;
        this.color = color;
    }
}
