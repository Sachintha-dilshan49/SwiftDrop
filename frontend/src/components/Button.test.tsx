import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Button } from "./Button";

describe("Button", () => {
  it("is a primary, non-submitting button by default", () => {
    render(<Button>Book delivery</Button>);

    const button = screen.getByRole("button", { name: "Book delivery" });
    expect(button).toHaveAttribute("type", "button");
    expect(button).toHaveClass("sd-button", "sd-button-primary");
  });

  it.each(["secondary", "danger", "ghost"] as const)("applies the %s variant", (variant) => {
    render(<Button variant={variant}>Go</Button>);

    expect(screen.getByRole("button", { name: "Go" })).toHaveClass(`sd-button-${variant}`);
  });

  it("keeps a submit type and extra class names", () => {
    render(<Button type="submit" className="booking-submit">Confirm</Button>);

    const button = screen.getByRole("button", { name: "Confirm" });
    expect(button).toHaveAttribute("type", "submit");
    expect(button).toHaveClass("sd-button", "booking-submit");
  });

  it("calls onClick unless it is disabled", () => {
    const onClick = vi.fn();
    const { rerender } = render(<Button onClick={onClick}>Save</Button>);

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(onClick).toHaveBeenCalledTimes(1);

    rerender(<Button onClick={onClick} disabled>Save</Button>);
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    expect(onClick).toHaveBeenCalledTimes(1);
  });
});
