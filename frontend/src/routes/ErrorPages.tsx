import { Link } from "./Link";
import "./ErrorPages.css";

type ErrorPageProps = {
  /** Where the button goes: the user's dashboard, or "/" when nobody is signed in. */
  homePath: string;
};

function ErrorPage({ code, title, text, homePath }: ErrorPageProps & { code: string; title: string; text: string }) {
  return (
    <main className="error-page">
      <section className="error-card" aria-labelledby="error-title">
        <Link className="error-brand" href="/">
          SwiftDrop
        </Link>
        <p className="error-code">{code}</p>
        <h1 id="error-title">{title}</h1>
        <p className="error-text">{text}</p>
        <Link className="error-home" href={homePath}>
          {homePath === "/" ? "Go to sign in" : "Go to my dashboard"}
        </Link>
      </section>
    </main>
  );
}

/** Shown when a signed-in user opens another role's portal (Figma "No access (403)"). */
export function ForbiddenPage({ homePath }: ErrorPageProps) {
  return (
    <ErrorPage
      code="403"
      title="You don’t have access to this page"
      text="This page belongs to another SwiftDrop portal. Go back to your own dashboard, or sign in with an account that has access."
      homePath={homePath}
    />
  );
}

/** Shown for any address the app does not know (Figma "Not found (404)"). */
export function NotFoundPage({ homePath }: ErrorPageProps) {
  return (
    <ErrorPage
      code="404"
      title="Page not found"
      text="The page you’re looking for doesn’t exist or has moved. Check the address, or use the button below."
      homePath={homePath}
    />
  );
}
