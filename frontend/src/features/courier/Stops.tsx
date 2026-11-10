/** Pickup (orange dot) and drop-off (dark dot), as in the Figma customer cards. */
export function Stops({ pickup, dropoff }: { pickup: string | null; dropoff: string | null }) {
  if (!pickup && !dropoff) {
    return <span className="courier-card-meta">Address not available</span>;
  }
  return (
    <span className="courier-stops">
      {pickup && <span><i className="courier-dot courier-dot-pickup" aria-hidden="true" />{pickup}</span>}
      {dropoff && <span><i className="courier-dot courier-dot-dropoff" aria-hidden="true" />{dropoff}</span>}
    </span>
  );
}
