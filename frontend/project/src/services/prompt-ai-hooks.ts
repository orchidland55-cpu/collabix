import { useQuery } from '@tanstack/react-query';
import { promptAIService } from './prompt-ai-service';

export function useAIPrompts() {
  return useQuery({
    queryKey: ['ai', 'prompts'],
    queryFn: () => promptAIService().listActive(),
  });
}
