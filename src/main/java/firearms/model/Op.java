package firearms.model;

/**
 * How a {@link Modifier} combines with a stat's running value during derivation. Every 1.0
 * attachment modifier in `docs/spec/domains/attach.md` §3 is {@code MULTIPLY}; {@code ADD} exists
 * because `docs/spec/domains/weapon.md` {@code WEAPON-REQ-003} requires the fold to be order
 * sensitive, and multiplication alone is commutative/associative and can never demonstrate that —
 * only a datapack-supplied or synthetic modifier mixing the two operators proves slot order matters
 * (`docs/spec/operations/testing.md`).
 */
public enum Op {
    ADD,
    MULTIPLY
}
