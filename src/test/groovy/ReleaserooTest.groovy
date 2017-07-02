import org.junit.Test

import java.nio.file.Path
import java.nio.file.Paths

class ReleaserooTest {

    @Test
    void testPath() {
        Path dist = Paths.get("/dist")

        Path path = Paths.get("/dist/static/foo.svg")
        Path pathRelative = dist.relativize(path)
        println pathRelative.nameCount
        println pathRelative
        println Paths.get("/gendi/").resolve(pathRelative)

        path = Paths.get("/dist/index.htm")
        pathRelative = dist.relativize(path)
        println pathRelative.nameCount

    }


}
