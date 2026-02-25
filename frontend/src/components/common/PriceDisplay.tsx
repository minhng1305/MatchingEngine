import clsx from 'clsx';

interface Props {
  price: number;
  previousPrice?: number;
  size?: 'sm' | 'md' | 'lg';
  showSign?: boolean;
}

export default function PriceDisplay({ price, previousPrice, size = 'md', showSign }: Props) {
  const direction = previousPrice ? (price > previousPrice ? 'up' : price < previousPrice ? 'down' : 'neutral') : 'neutral';
  const change = previousPrice ? ((price - previousPrice) / previousPrice) * 100 : 0;

  const sizeClass = {
    sm: 'text-xs',
    md: 'text-sm',
    lg: 'text-lg font-semibold',
  }[size];

  return (
    <span
      className={clsx(
        'font-mono tabular-nums transition-colors duration-300',
        sizeClass,
        direction === 'up' && 'price-up',
        direction === 'down' && 'price-down',
        direction === 'neutral' && 'text-gray-200'
      )}
    >
      {showSign && direction === 'up' && '+'}
      ${price.toFixed(2)}
      {previousPrice !== undefined && change !== 0 && (
        <span className="ml-1 text-2xs">
          ({change > 0 ? '+' : ''}{change.toFixed(2)}%)
        </span>
      )}
    </span>
  );
}
