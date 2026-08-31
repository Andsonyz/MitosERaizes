package br.com.entremitoseraizes;

/** Verifica o ciclo de todas as missões sem abrir a janela do jogo. */
public final class GameSessionSmokeTest {
    private GameSessionSmokeTest() { }

    public static void main(String[] args) {
        GameSession session = new GameSession();
        int[] expectedTasks = { 3, 3, 3, 4 };
        for (int chapter = 0; chapter <= GameSession.LAST_CHAPTER; chapter++) {
            if (session.remainingTasks().size() != expectedTasks[chapter]) {
                throw new AssertionError("Quantidade de tarefas incorreta no capítulo " + chapter);
            }
            boolean completedChapter = false;
            while (!session.remainingTasks().isEmpty()) {
                Task task = session.remainingTasks().get(0);
                if (session.isBlocked(task.x, task.y)) {
                    throw new AssertionError("Tarefa fora de uma área acessível: " + task.id);
                }
                completedChapter = session.complete(task);
            }
            if (!completedChapter) throw new AssertionError("Capítulo não foi concluído: " + chapter);
            if (chapter < GameSession.LAST_CHAPTER) session.nextChapter();
        }
        System.out.println("Ciclo de missões validado: 4 capítulos e 13 ações.");
    }
}
