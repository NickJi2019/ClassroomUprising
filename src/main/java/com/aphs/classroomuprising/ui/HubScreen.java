package com.aphs.classroomuprising.ui;

import com.aphs.classroomuprising.DayPeriod;
import com.aphs.classroomuprising.GameState;
import com.aphs.classroomuprising.RuleValues;
import com.aphs.classroomuprising.StatChange;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Student Hub slots (and the early-sleep morning bonus). The menu lists the configured
 * activities plus whichever sleep option the core accepts in this period; the side card
 * previews configured values. Validity is still decided by GameController.
 */
final class HubScreen extends Screen {
    /** One menu entry with its preview card. */
    private record Option(String label, String cost, List<StatDelta> effects, String caution,
                          String tone, Supplier<Outcome> action) {}

    @FXML private Label place;
    @FXML private ChoiceList options;
    @FXML private Label detailTitle;
    @FXML private Label detailCost;
    @FXML private FlowPane detailEffects;
    @FXML private Label detailCaution;
    @FXML private VBox notice;
    @FXML private Hud hud;

    private List<Option> current = List.of();

    HubScreen(AppContext ctx) { super(ctx); }

    @Override
    protected Parent build() {
        StackPane root = Views.load("hub.fxml", this);
        root.getChildren().add(0, new Backdrop(
                new Backdrop.Glyph("STUDENT HUB", 420, 300, 170, true),
                new Backdrop.Glyph("while (awake)", 760, 90, 58, false)));
        options.setOnSelect(this::preview);
        return root;
    }

    @Override
    protected boolean allowsPause() { return true; }

    @Override
    protected void focus() { options.requestFocus(); }

    @Override
    protected void onShown() {
        GameState view = ctx.session().view();
        hud.show(view, ctx.demoLabel());
        place.setText(view.period() == DayPeriod.MORNING_BONUS ? "MORNING  ·  BONUS TIME" : "STUDENT HUB  ·  " + UiText.period(view.period()));
        current = optionsFor(view);
        options.setChoices(current.stream()
                .map(o -> ChoiceList.Choice.of(o.label(), null, () -> act(o)).tone(o.tone()))
                .toList());
    }

    private List<Option> optionsFor(GameState view) {
        RuleValues rules = ctx.session().rules();
        DayPeriod period = view.period();
        double scale = period.activityUnits();
        var list = new ArrayList<Option>();
        for (String id : ctx.session().hubActivityIds()) {
            Fetch<StatChange> effect = ctx.session().hubEffect(id);
            List<StatDelta> deltas = effect.ok() ? StatDelta.of(effect.value(), scale) : List.of();
            String cost = scale < 1 ? "Uses the ½ bonus slot · half effect" : "Uses 1 time slot";
            list.add(new Option(UiText.hubActivity(id), cost, deltas, caution(view, rules), "normal",
                    () -> ctx.session().hubActivity(id)));
        }
        if (period == DayPeriod.AFTER_SCHOOL) {
            list.add(new Option("SLEEP NOW", "Ends today right after school", List.of(),
                    "Tomorrow: SAN " + value(rules.earlySleepSanBonus(), "+") + " and a ½ bonus slot before class.",
                    "cyan", ctx.session()::sleepAfterSchool));
        }
        if (period == DayPeriod.EVENING || period == DayPeriod.LATE_NIGHT) {
            list.add(new Option("SLEEP", "Ends today", List.of(),
                    "Sleeping recovers SAN " + value(rules.normalSleepSanRecovery(), "+") + ".",
                    "cyan", ctx.session()::sleepNormally));
        }
        return List.copyOf(list);
    }

    /** Explains rules that change what an activity does right now, using configured values. */
    private static String caution(GameState view, RuleValues rules) {
        var parts = new ArrayList<String>();
        if (view.period() == DayPeriod.LATE_NIGHT) {
            parts.add(view.isTutorial()
                    ? "Staying up late: no penalty during the tutorial."
                    : "Staying up late: tomorrow SAN " + value(rules.lateNightSanReduction(), "−")
                      + ", XP gain ×" + value(rules.lateNightXpMultiplier(), "") + ".");
        }
        if (view.xpMultiplier() < 1) parts.add("XP gain today ×" + StatDelta.number(view.xpMultiplier()) + ".");
        if (view.isTutorial()) parts.add("Tutorial: negative effects are warnings only.");
        return String.join("\n", parts);
    }

    private static String value(Double configured, String sign) {
        return configured == null ? "(pending)" : sign + StatDelta.number(configured);
    }

    private void preview(int index) {
        if (index < 0 || index >= current.size()) return;
        Option option = current.get(index);
        detailTitle.setText(option.label());
        detailCost.setText(option.cost());
        detailEffects.getChildren().clear();
        for (StatDelta delta : option.effects()) {
            Label chip = new Label(delta.text());
            chip.getStyleClass().addAll("display", "effect-chip", delta.positive() ? "effect-up" : "effect-down");
            detailEffects.getChildren().add(chip);
        }
        detailCaution.setText(option.caution());
    }

    private void act(Option option) {
        Outcome outcome = option.action().get();
        if (outcome instanceof Outcome.Done done) {
            ctx.toasts().outcome(done);
            ctx.advance();
        } else {
            notice(notice, Diagnostic.of(outcome));
            options.unlock();
        }
    }
}
