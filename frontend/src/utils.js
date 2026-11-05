export const CATEGORIES = ["TRAVEL", "FOOD", "ACCOMMODATION", "OFFICE_SUPPLIES", "EQUIPMENT", "TRAINING", "OTHER"];
export const STATUSES = ["PENDING", "APPROVED", "REJECTED"];

export const label = (v) =>
  v ? v.toLowerCase().replace(/_/g, " ").replace(/^\w/, (c) => c.toUpperCase()) : "";

const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });
export const formatMoney = (n) => money.format(Number(n || 0));

export const formatDate = (d) =>
  d ? new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" }) : "";

export const formatDateTime = (d) =>
  d
    ? new Date(d).toLocaleString("en-GB", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })
    : "";

export const todayISO = () => new Date().toISOString().slice(0, 10);

/** Turns an Axios error into a message a person can act on. */
export function errMsg(e, fallback = "Something went wrong. Please try again.") {
  const d = e?.response?.data;
  if (d && typeof d === "object" && !(d instanceof Blob)) {
    if (d.fieldErrors && Object.keys(d.fieldErrors).length) return Object.values(d.fieldErrors).join(". ");
    if (d.message) return d.message;
  }
  if (e?.request && !e?.response) return "Cannot reach the server. Check that the backend is running.";
  return fallback;
}

export const RECEIPT_TYPES = ["image/jpeg", "image/png", "application/pdf"];
export const MAX_RECEIPT_BYTES = 5 * 1024 * 1024;

export function validateReceipt(file) {
  if (!file) return null;
  if (!RECEIPT_TYPES.includes(file.type)) return "Receipt must be a JPEG, PNG or PDF file";
  if (file.size > MAX_RECEIPT_BYTES) return "Receipt must be 5 MB or smaller";
  return null;
}
