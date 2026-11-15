import type { ButtonHTMLAttributes } from "react";
import "./Button.css";

export type ButtonVariant = "primary" | "secondary" | "danger" | "ghost";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
};

/** Shared button (Figma "shared components" frame): primary, secondary, danger and ghost. */
export function Button({ variant = "primary", type = "button", className, ...rest }: ButtonProps) {
  const classes = ["sd-button", `sd-button-${variant}`, className].filter(Boolean).join(" ");
  return <button type={type} className={classes} {...rest} />;
}
