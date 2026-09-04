package br.com.entremitoseraizes.game;

/** Cada etapa corresponde a uma mecânica apresentada na proposta da fase. */
public enum Stage {
    FIND_CABIN("Siga a trilha até a cabana do Curupira."),
    TALK_TO_CURUPIRA("Entre na cabana e converse com o Curupira."),
    COMPLETE("Fase concluída."),
    RETURN_TO_PT2("Volte para a entrada da cabana."),
    ACCESS_UNLOCKED_FOREST("Atravesse a clareira recém-descoberta."),
    REACH_PT4("Siga a trilha até a próxima área."),
    REACH_PT5("Continue avançando pela floresta."),
    REACH_PT6("Encontre a origem da caça ilegal."),
    BATTLE_VILLAIN("Enfrente o caçador ilegal."),
    PHASE_COMPLETE("Fase concluída.");

    private final String objective;

    Stage(String objective) { this.objective = objective; }
    String objective() { return objective; }
}
