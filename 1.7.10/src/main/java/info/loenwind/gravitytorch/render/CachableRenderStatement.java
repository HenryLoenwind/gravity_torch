package info.loenwind.gravitytorch.render;

public interface CachableRenderStatement {

  void execute(RenderingContext renderingContext);

  void execute_tesselated(RenderingContext renderingContext);

}
