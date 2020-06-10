package io.crumbl

package object encrypter {
  type Crumbs = Seq[Crumb]
  def Crumbs(xs: Crumb*): Seq[Crumb] = Seq[Crumb](xs: _*)

  object Crumbs {

    /**
     * Return the crumbs at the specified index from the passed crumbs
     */
    def getAt(index: Int, in: Crumbs): Seq[Crumb] =
      in.filter(_.index == index)

  }
}
