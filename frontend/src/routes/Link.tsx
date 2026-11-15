import type { AnchorHTMLAttributes, MouseEvent } from "react";
import { navigate } from "./router";

type LinkProps = AnchorHTMLAttributes<HTMLAnchorElement> & { href: string };

/** An in-app link: a normal click navigates without reloading; Ctrl/Cmd-click and middle-click still open a new tab. */
export function Link({ href, onClick, ...rest }: LinkProps) {
  function handleClick(event: MouseEvent<HTMLAnchorElement>) {
    onClick?.(event);
    const modified = event.metaKey || event.ctrlKey || event.shiftKey || event.altKey;
    if (event.defaultPrevented || event.button !== 0 || modified || rest.target) {
      return;
    }
    event.preventDefault();
    navigate(href);
  }

  return <a href={href} onClick={handleClick} {...rest} />;
}
