package br.com.entremitoseraizes.game;

/** Cada etapa corresponde a uma mecânica apresentada na proposta da fase. */
public enum Stage {
    FIND_CABIN("Siga a trilha até a cabana do Curupira."),
    TALK_TO_CURUPIRA("Entre na cabana e converse com o Curupira."),
    COMPLETE("Fase concluída.");

    private final String objective;

    Stage(String objective) { this.objective = objective; }
    String objective() { return objective; }
}
