import React from "react";
import ReactDOM from "react-dom/client";
import { AuthApp } from "./auth/AuthApp";
import { PublicTrackingPage } from "./features/tracking/PublicTrackingPage";
import { trackingNumberFromPath } from "./features/tracking/trackingNumber";
import "./style.css";

// /track and /track/{trackingNo} are public (tracking links in emails and SMS); everything else needs login.
const trackingNo = trackingNumberFromPath(window.location.pathname);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    {trackingNo === null ? <AuthApp /> : <PublicTrackingPage initialTrackingNo={trackingNo} />}
  </React.StrictMode>,
);
