package firearms.model;

/**
 * One attachment's effect on one stat: {@code stat op value}, folded onto the running total by
 * {@link StatDerivation} in fixed slot order (`docs/spec/domains/weapon.md` {@code
 * WEAPON-REQ-003}). An attachment carries a list of these, one per stat it touches
 * (`docs/spec/domains/attach.md` §3).
 */
public record Modifier(Stat stat, Op op, double value) {

    /** A multiplicative modifier — every 1.0 attachment modifier is this shape. */
    public static Modifier multiply(Stat stat, double factor) {
        return new Modifier(stat, Op.MULTIPLY, factor);
    }

    /** An additive modifier; no 1.0 attachment uses this, kept for `Op.ADD`'s own order-sensitivity proof. */
    public static Modifier add(Stat stat, double amount) {
        return new Modifier(stat, Op.ADD, amount);
    }

    /** Applies this modifier to a running value: {@code ADD} sums, {@code MULTIPLY} scales. */
    double apply(double runningValue) {
        return switch (op) {
            case ADD -> runningValue + value;
            case MULTIPLY -> runningValue * value;
        };
    }
}
