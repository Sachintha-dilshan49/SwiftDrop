package lk.swiftdrop.pricing.rule.entity;

/**
 * The kinds of pricing rule and what their {@code ruleKey} names (docs/architecture.md §8).
 *
 * <ul>
 *   <li>{@code ROUTE_BASE}: a route type; the amount is the base fee.</li>
 *   <li>{@code EXTRA_KG}: always {@code PER_KG}; the amount is charged per started kg after the first.</li>
 *   <li>{@code SIZE}: a parcel size; the amount is its surcharge, and the rule also holds the size's maximum weight.</li>
 *   <li>{@code SPEED}: a delivery speed; the amount is its surcharge.</li>
 * </ul>
 */
public enum RuleType {
    ROUTE_BASE,
    EXTRA_KG,
    SIZE,
    SPEED
}
