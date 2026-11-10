package lk.swiftdrop.pricing.quote.fee;

import lk.swiftdrop.pricing.quote.model.RouteType;
import lk.swiftdrop.pricing.zone.entity.ZoneGroup;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** The base fee of the route between the two zones. */
@Component
@Order(FeeOrder.ROUTE_BASE)
public class RouteBaseFee implements FeeComponent {

    @Override
    public void apply(FeeContext context, FeeBreakdown.Builder breakdown) {
        RouteType routeType = routeType(context.pickup(), context.dropoff());
        breakdown.routeType(routeType).baseFee(context.rules().routeBaseFee(routeType));
    }

    /**
     * Checked in this order (docs/architecture.md §8): inter-city if either end is inter-city, then the same
     * zone, then two zones of one group, and otherwise one Colombo and one suburbs zone.
     */
    static RouteType routeType(ZoneRef pickup, ZoneRef dropoff) {
        if (pickup.group() == ZoneGroup.INTER_CITY || dropoff.group() == ZoneGroup.INTER_CITY) {
            return RouteType.INTER_CITY;
        }
        if (pickup.code().equals(dropoff.code())) {
            return RouteType.SAME_ZONE;
        }
        if (pickup.group() == dropoff.group()) {
            return RouteType.NEIGHBOURING;
        }
        return RouteType.COLOMBO_SUBURBS;
    }
}
