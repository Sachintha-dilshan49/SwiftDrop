import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";
import { Link } from "./Link";
import { navigate, usePath } from "./router";

function ShowPath() {
  return <p>path: {usePath()}</p>;
}

describe("router", () => {
  beforeEach(() => {
    window.history.replaceState(null, "", "/");
  });

  it("re-renders on navigate and adds a history entry", () => {
    render(<ShowPath />);
    const lengthBefore = window.history.length;

    act(() => navigate("/customer"));

    expect(screen.getByText("path: /customer")).toBeInTheDocument();
    expect(window.history.length).toBe(lengthBefore + 1);
  });

  it("replaces the current entry when asked", () => {
    render(<ShowPath />);
    const lengthBefore = window.history.length;

    act(() => navigate("/admin", { replace: true }));

    expect(screen.getByText("path: /admin")).toBeInTheDocument();
    expect(window.history.length).toBe(lengthBefore);
  });

  it("follows the browser's Back and Forward buttons", () => {
    render(<ShowPath />);

    act(() => {
      window.history.replaceState(null, "", "/courier");
      window.dispatchEvent(new PopStateEvent("popstate"));
    });

    expect(screen.getByText("path: /courier")).toBeInTheDocument();
  });

  it("navigates on a plain Link click without reloading", () => {
    render(
      <>
        <ShowPath />
        <Link href="/merchant">Merchant</Link>
      </>,
    );

    fireEvent.click(screen.getByRole("link", { name: "Merchant" }));

    expect(screen.getByText("path: /merchant")).toBeInTheDocument();
  });

  it("leaves Ctrl-click to the browser so it can open a new tab", () => {
    render(
      <>
        <ShowPath />
        <Link href="/merchant">Merchant</Link>
      </>,
    );

    fireEvent.click(screen.getByRole("link", { name: "Merchant" }), { ctrlKey: true });

    expect(screen.getByText("path: /")).toBeInTheDocument();
  });
});
