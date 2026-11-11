package com.aphs.classroomuprising;

/** Test-only real defeat path for later-day tests; no production skip/CLI/debug entry is added. */
public final class OpeningTestSupport {
    private OpeningTestSupport() { }
    public static void enterDayOne(GameController game) {
        game.startOpeningBattle();
        game.hitOpeningQuestion("csa2014-q15");
        game.startOpeningQuestion("csa2014-q15");
        game.answerOpeningQuestion("csa2014-q15", "B");
        if (game.openingBattle().ending() == null || game.openingBattle().ending().grade() != 67)
            throw new AssertionError("Expected the authored opening defeat.");
        game.completeOpening();
    }
}
