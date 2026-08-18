export const IconKitSelect = ({ label, value, options, onChange }) => {
  const [open, setOpen] = useState(false);
  const selected = options.find((option) => option.value === value) ?? options[0];

  return (
    <div className="min-w-0 space-y-2">
      <p className="m-0 text-sm font-medium text-zinc-950 dark:text-white">{label}</p>
      <div className="relative">
        <button
          type="button"
          aria-haspopup="listbox"
          aria-expanded={open}
          onClick={() => setOpen(!open)}
          className="flex h-10 w-full items-center justify-between rounded-lg border border-zinc-950/15 bg-white px-3 text-sm text-zinc-950 shadow-sm transition hover:border-zinc-950/35 focus:outline-none dark:border-white/20 dark:bg-zinc-900 dark:text-white"
        >
          <span>{selected.label}</span>
          <svg
            aria-hidden="true"
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className={`lucide lucide-chevron-down h-4 w-4 text-zinc-500 transition-transform duration-200 dark:text-zinc-400 ${open ? "rotate-180" : ""}`}
          >
            <path d="m6 9 6 6 6-6" />
          </svg>
        </button>
        {open && (
          <>
            <div
              className="fixed inset-0 z-10"
              onClick={() => setOpen(false)}
            />
            <div
              role="listbox"
              className="absolute z-20 mt-1 w-full overflow-hidden rounded-lg border border-zinc-950/15 bg-white p-1 shadow-lg ring-1 ring-black/5 dark:border-white/20 dark:bg-zinc-900 dark:ring-white/10"
            >
              {options.map((option) => (
                <button
                  type="button"
                  role="option"
                  aria-selected={option.value === value}
                  key={option.value}
                  onClick={() => {
                    onChange(option.value);
                    setOpen(false);
                  }}
                  className={`flex min-h-9 w-full items-center justify-between rounded-md px-2.5 text-left text-sm transition ${option.value === value
                      ? "bg-zinc-100 font-medium text-zinc-950 dark:bg-white/15 dark:text-white"
                      : "text-zinc-700 hover:bg-zinc-50 dark:text-zinc-300 dark:hover:bg-white/5"
                    }`}
                >
                  <span>{option.label}</span>
                  {option.value === value ? (
                    <svg
                      aria-hidden="true"
                      xmlns="http://www.w3.org/2000/svg"
                      width="16"
                      height="16"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      className="lucide lucide-check h-4 w-4 shrink-0 text-zinc-900 dark:text-zinc-100"
                    >
                      <path d="M20 6 9 17l-5-5" />
                    </svg>
                  ) : (
                    <span className="h-4 w-4" />
                  )}
                </button>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export const IconKitStepper = ({ label, value, step, min, max, onChange }) => {
  const change = (next) => onChange(Math.max(min, Math.min(max, next)));

  return (
    <div className="min-w-0 space-y-2">
      <p className="m-0 text-sm font-medium text-zinc-950 dark:text-white">{label}</p>
      <div className="flex h-10 w-full overflow-hidden rounded-lg border border-zinc-950/15 bg-white shadow-sm dark:border-white/20 dark:bg-zinc-900">
        <button
          type="button"
          aria-label={`减少${label}`}
          onClick={() => change(value - step)}
          className="flex h-full w-10 shrink-0 items-center justify-center border-r border-zinc-950/15 text-zinc-600 transition hover:bg-zinc-100 dark:border-white/20 dark:text-zinc-300 dark:hover:bg-white/10"
        >
          <svg
            aria-hidden="true"
            xmlns="http://www.w3.org/2000/svg"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="lucide lucide-minus"
          >
            <path d="M5 12h14" />
          </svg>
        </button>
        <input
          value={`${value}%`}
          inputMode="numeric"
          aria-label={`${label}百分比`}
          onChange={(event) => {
            const next = Number(event.target.value.replace("%", ""));
            if (Number.isFinite(next)) change(next);
          }}
          className="h-full min-w-0 flex-1 border-0 bg-transparent px-1 text-center text-sm font-medium text-zinc-950 outline-none dark:text-white"
        />
        <button
          type="button"
          aria-label={`增加${label}`}
          onClick={() => change(value + step)}
          className="flex h-full w-10 shrink-0 items-center justify-center border-l border-zinc-950/15 text-zinc-600 transition hover:bg-zinc-100 dark:border-white/20 dark:text-zinc-300 dark:hover:bg-white/10"
        >
          <svg
            aria-hidden="true"
            xmlns="http://www.w3.org/2000/svg"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            className="lucide lucide-plus"
          >
            <path d="M5 12h14" />
            <path d="M12 5v14" />
          </svg>
        </button>
      </div>
    </div>
  );
};
