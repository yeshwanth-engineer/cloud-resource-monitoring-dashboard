/** Inline message strip for errors and notices. `tone` picks the colour. */
export function Banner({ tone = 'error', children }) {
  if (!children) return null;
  return (
    <div className={`banner banner-${tone}`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
