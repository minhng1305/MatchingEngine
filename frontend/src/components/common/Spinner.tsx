import clsx from 'clsx';

export default function Spinner({ className = '' }: { className?: string }) {
  return (
    <div className={clsx('flex items-center justify-center', className)}>
      <div className="w-6 h-6 border-2 border-accent/30 border-t-accent rounded-full animate-spin" />
    </div>
  );
}
