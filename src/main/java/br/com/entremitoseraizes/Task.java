package br.com.entremitoseraizes;

/** Um ponto interativo associado à missão em andamento. */
final class Task {
    final String id;
    final int x;
    final int y;
    final String label;
    final String detail;

    Task(String id, int x, int y, String label, String detail) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.label = label;
        this.detail = detail;
    }
}
