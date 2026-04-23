/**
 * RichText component - parses markdown-like formatting:
 * **bold text**
 * `inline code`
 */

interface RichTextProps {
  text: string;
  className?: string;
}

export const RichText = ({ text, className = '' }: RichTextProps) => {
  // Split text into parts: bold, code, and regular text
  const parts: (string | { type: 'bold' | 'code'; content: string })[] = [];
  let remaining = text;
  let match;

  const boldRegex = /\*\*([^\*]+)\*\*/g;
  const codeRegex = /`([^`]+)`/g;
  const combinedRegex = /(\*\*[^\*]+\*\*|`[^`]+`)/g;

  let lastIndex = 0;
  const allMatches = Array.from(text.matchAll(combinedRegex));

  if (allMatches.length === 0) {
    // No formatting found
    return <span className={className}>{text}</span>;
  }

  allMatches.forEach((match) => {
    const content = match[0];
    const startIndex = match.index!;

    // Add text before this match
    if (startIndex > lastIndex) {
      parts.push(text.substring(lastIndex, startIndex));
    }

    // Parse the match
    if (content.startsWith('**') && content.endsWith('**')) {
      parts.push({
        type: 'bold',
        content: content.slice(2, -2),
      });
    } else if (content.startsWith('`') && content.endsWith('`')) {
      parts.push({
        type: 'code',
        content: content.slice(1, -1),
      });
    }

    lastIndex = startIndex + content.length;
  });

  // Add remaining text
  if (lastIndex < text.length) {
    parts.push(text.substring(lastIndex));
  }

  return (
    <span className={className}>
      {parts.map((part, i) => {
        if (typeof part === 'string') {
          return <span key={i}>{part}</span>;
        }

        if (part.type === 'bold') {
          return <strong key={i} className="font-semibold">{part.content}</strong>;
        }

        if (part.type === 'code') {
          return (
            <code
              key={i}
              className="bg-muted/60 px-1.5 py-0.5 rounded text-[0.9em] font-mono text-xs"
            >
              {part.content}
            </code>
          );
        }

        return null;
      })}
    </span>
  );
};
