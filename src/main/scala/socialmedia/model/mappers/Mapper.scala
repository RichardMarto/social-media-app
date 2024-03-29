package socialmedia.model.mappers

trait Mapper[M, P] {
  def toModel(proto: P): M
  def toProto(model: M): P
}
