export interface Renderer<T> {
  render(model: T): string;
}
