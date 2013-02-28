package net.minecraft

package object util {
	implicit class HTMLC(val sc: StringContext) extends AnyVal {
		def html(args: Any*): String = {
			val html = sc standardInterpolator (_.replace("\n", "<br/>"), args)
			s"<html>$html</html>"
		}
	}
}
