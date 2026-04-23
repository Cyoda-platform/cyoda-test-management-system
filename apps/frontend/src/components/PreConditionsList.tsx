/**
 * PreConditionsList component - displays pre-conditions with checkmarks
 * Each line is treated as a separate pre-condition
 */

import { Check } from 'lucide-react';
import { RichText } from './RichText';

interface PreConditionsListProps {
  text: string;
}

export const PreConditionsList = ({ text }: PreConditionsListProps) => {
  // Split by newlines and filter out empty lines
  const conditions = text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  // If only one line, just display with checkbox
  if (conditions.length === 1) {
    return (
      <div className="flex items-start gap-2.5">
        <Check className="h-4 w-4 text-purple-600 shrink-0 mt-0.5" strokeWidth={2.5} />
        <RichText text={conditions[0]} className="text-sm text-foreground leading-relaxed" />
      </div>
    );
  }

  // Multiple conditions - display as list
  return (
    <div className="space-y-2">
      {conditions.map((condition, index) => (
        <div key={index} className="flex items-start gap-2.5">
          <Check className="h-4 w-4 text-purple-600 shrink-0 mt-0.5" strokeWidth={2.5} />
          <RichText text={condition} className="text-sm text-foreground leading-relaxed" />
        </div>
      ))}
    </div>
  );
};
