/*
 * Copyright (C) 2009-2017 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.http.scaladsl.server.directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server._
import docs.http.scaladsl.server.RoutingSpec

class PathDirectivesExamplesSpec extends RoutingSpec {

  //#path-matcher
  val matcher: PathMatcher1[Option[Int]] =
    "foo" / "bar" / "X" ~ IntNumber.? / ("edit" | "create")
  //#path-matcher

  //#path-dsl
  // matches /foo/
  path("foo"./)

  // matches e.g. /foo/123 and extracts "123" as a String
  path("foo" / """\d+""".r)

  // matches e.g. /foo/bar123 and extracts "123" as a String
  path("foo" / """bar(\d+)""".r)

  // similar to `path(Segments)`
  path(Segment.repeat(10, separator = Slash))

  // matches e.g. /i42 or /hCAFE and extracts an Int
  path("i" ~ IntNumber | "h" ~ HexIntNumber)

  // identical to path("foo" ~ (PathEnd | Slash))
  path("foo" ~ Slash.?)

  // matches /red or /green or /blue and extracts 1, 2 or 3 respectively
  path(Map("red" -> 1, "green" -> 2, "blue" -> 3))

  // matches anything starting with "/foo" except for /foobar
  pathPrefix("foo" ~ !"bar")
  //#path-dsl

  //#completeWithUnmatchedPath
  val completeWithUnmatchedPath =
    extractUnmatchedPath { p =>
      complete(p.toString)
    }

  //#completeWithUnmatchedPath

  "path-example" in {
    //#path-example
    val route =
      path("foo") {
        complete("/foo")
      } ~
        path("foo" / "bar") {
          complete("/foo/bar")
        } ~
        pathPrefix("ball") {
          pathEnd {
            complete("/ball")
          } ~
            path(IntNumber) { int =>
              complete(if (int % 2 == 0) "even ball" else "odd ball")
            }
        }

    // tests:
    Get("/") ~> route ~> check {
      handled shouldEqual false
    }

    Get("/foo") ~> route ~> check {
      responseAs[String] shouldEqual "/foo"
    }

    Get("/foo/bar") ~> route ~> check {
      responseAs[String] shouldEqual "/foo/bar"
    }

    Get("/ball/1337") ~> route ~> check {
      responseAs[String] shouldEqual "odd ball"
    }
    //#path-example
  }

  "pathEnd-" in {
    //#pathEnd-
    val route =
      pathPrefix("foo") {
        pathEnd {
          complete("/foo")
        } ~
          path("bar") {
            complete("/foo/bar")
          }
      }

    // tests:
    Get("/foo") ~> route ~> check {
      responseAs[String] shouldEqual "/foo"
    }

    Get("/foo/") ~> route ~> check {
      handled shouldEqual false
    }

    Get("/foo/bar") ~> route ~> check {
      responseAs[String] shouldEqual "/foo/bar"
    }
    //#pathEnd-
  }

  "pathEndOrSingleSlash-" in {
    //#pathEndOrSingleSlash-
    val route =
      pathPrefix("foo") {
        pathEndOrSingleSlash {
          complete("/foo")
        } ~
          path("bar") {
            complete("/foo/bar")
          }
      }

    // tests:
    Get("/foo") ~> route ~> check {
      responseAs[String] shouldEqual "/foo"
    }

    Get("/foo/") ~> route ~> check {
      responseAs[String] shouldEqual "/foo"
    }

    Get("/foo/bar") ~> route ~> check {
      responseAs[String] shouldEqual "/foo/bar"
    }
    //#pathEndOrSingleSlash-
  }

  "pathPrefix-" in {
    //#pathPrefix-
    val route =
      pathPrefix("ball") {
        pathEnd {
          complete("/ball")
        } ~
          path(IntNumber) { int =>
            complete(if (int % 2 == 0) "even ball" else "odd ball")
          }
      }

    // tests:
    Get("/") ~> route ~> check {
      handled shouldEqual false
    }

    Get("/ball") ~> route ~> check {
      responseAs[String] shouldEqual "/ball"
    }

    Get("/ball/1337") ~> route ~> check {
      responseAs[String] shouldEqual "odd ball"
    }
    //#pathPrefix-
  }

  "pathPrefixTest-" in {
    //#pathPrefixTest-
    val route =
      pathPrefixTest("foo" | "bar") {
        pathPrefix("foo") { completeWithUnmatchedPath } ~
          pathPrefix("bar") { completeWithUnmatchedPath }
      }

    // tests:
    Get("/foo/doo") ~> route ~> check {
      responseAs[String] shouldEqual "/doo"
    }

    Get("/bar/yes") ~> route ~> check {
      responseAs[String] shouldEqual "/yes"
    }
    //#pathPrefixTest-
  }

  "pathSingleSlash-" in {
    //#pathSingleSlash-
    val route =
      pathSingleSlash {
        complete("root")
      } ~
        pathPrefix("ball") {
          pathSingleSlash {
            complete("/ball/")
          } ~
            path(IntNumber) { int =>
              complete(if (int % 2 == 0) "even ball" else "odd ball")
            }
        }

    // tests:
    Get("/") ~> route ~> check {
      responseAs[String] shouldEqual "root"
    }

    Get("/ball") ~> route ~> check {
      handled shouldEqual false
    }

    Get("/ball/") ~> route ~> check {
      responseAs[String] shouldEqual "/ball/"
    }

    Get("/ball/1337") ~> route ~> check {
      responseAs[String] shouldEqual "odd ball"
    }
    //#pathSingleSlash-
  }

  "pathSuffix-" in {
    //#pathSuffix-
    val route =
      pathPrefix("start") {
        pathSuffix("end") {
          completeWithUnmatchedPath
        } ~
          pathSuffix("foo" / "bar" ~ "baz") {
            completeWithUnmatchedPath
          }
      }

    // tests:
    Get("/start/middle/end") ~> route ~> check {
      responseAs[String] shouldEqual "/middle/"
    }

    Get("/start/something/barbaz/foo") ~> route ~> check {
      responseAs[String] shouldEqual "/something/"
    }
    //#pathSuffix-
  }

  "pathSuffixTest-" in {
    //#pathSuffixTest-
    val route =
      pathSuffixTest(Slash) {
        complete("slashed")
      } ~
        complete("unslashed")

    // tests:
    Get("/foo/") ~> route ~> check {
      responseAs[String] shouldEqual "slashed"
    }
    Get("/foo") ~> route ~> check {
      responseAs[String] shouldEqual "unslashed"
    }
    //#pathSuffixTest-
  }

  "rawPathPrefix-" in {
    //#rawPathPrefix-
    val route =
      pathPrefix("foo") {
        rawPathPrefix("bar") { completeWithUnmatchedPath } ~
          rawPathPrefix("doo") { completeWithUnmatchedPath }
      }

    // tests:
    Get("/foobar/baz") ~> route ~> check {
      responseAs[String] shouldEqual "/baz"
    }

    Get("/foodoo/baz") ~> route ~> check {
      responseAs[String] shouldEqual "/baz"
    }
    //#rawPathPrefix-
  }

  "rawPathPrefixTest-" in {
    //#rawPathPrefixTest-
    val route =
      pathPrefix("foo") {
        rawPathPrefixTest("bar") {
          completeWithUnmatchedPath
        }
      }

    // tests:
    Get("/foobar") ~> route ~> check {
      responseAs[String] shouldEqual "bar"
    }

    Get("/foobaz") ~> route ~> check {
      handled shouldEqual false
    }
    //#rawPathPrefixTest-
  }

  "redirectToTrailingSlashIfMissing-0" in {
    //#redirectToTrailingSlashIfMissing-0
    import akka.http.scaladsl.model.StatusCodes

    val route =
      redirectToTrailingSlashIfMissing(StatusCodes.MovedPermanently) {
        path("foo"./) {
          // We require the explicit trailing slash in the path
          complete("OK")
        } ~
          path("bad-1") {
            // MISTAKE!
            // Missing `/` in path, causes this path to never match,
            // because it is inside a `redirectToTrailingSlashIfMissing`
            ???
          } ~
          path("bad-2/") {
            // MISTAKE!
            // / should be explicit as path element separator and not *in* the path element
            // So it should be: "bad-2" /
            ???
          }
      }

    // tests:
    // Redirected:
    Get("/foo") ~> route ~> check {
      status shouldEqual StatusCodes.MovedPermanently

      // results in nice human readable message,
      // in case the redirect can't be followed automatically:
      responseAs[String] shouldEqual {
        "This and all future requests should be directed to " +
          "<a href=\"http://example.com/foo/\">this URI</a>."
      }
    }

    // Properly handled:
    Get("/foo/") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }

    // MISTAKE! will never match - reason explained in routes
    Get("/bad-1/") ~> route ~> check {
      handled shouldEqual false
    }

    // MISTAKE! will never match - reason explained in routes
    Get("/bad-2/") ~> route ~> check {
      handled shouldEqual false
    }
    //#redirectToTrailingSlashIfMissing-0
  }

  "redirectToNoTrailingSlashIfPresent-0" in {
    //#redirectToNoTrailingSlashIfPresent-0
    import akka.http.scaladsl.model.StatusCodes

    val route =
      redirectToNoTrailingSlashIfPresent(StatusCodes.MovedPermanently) {
        path("foo") {
          // We require to not have a trailing slash in the path
          complete("OK")
        } ~
          path("bad"./) {
            // MISTAKE!
            // Since inside a `redirectToNoTrailingSlashIfPresent` directive
            // the matched path here will never contain a trailing slash,
            // thus this path will never match.
            //
            // It should be `path("bad")` instead.
            ???
          }
      }

    // tests:
    // Redirected:
    Get("/foo/") ~> route ~> check {
      status shouldEqual StatusCodes.MovedPermanently

      // results in nice human readable message,
      // in case the redirect can't be followed automatically:
      responseAs[String] shouldEqual {
        "This and all future requests should be directed to " +
          "<a href=\"http://example.com/foo\">this URI</a>."
      }
    }

    // Properly handled:
    Get("/foo") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }

    // MISTAKE! will never match - reason explained in routes
    Get("/bad") ~> route ~> check {
      handled shouldEqual false
    }
    //#redirectToNoTrailingSlashIfPresent-0
  }

  "ignoreTrailingSlash" in {
    //#ignoreTrailingSlash
    val route = ignoreTrailingSlash {
      path("foo") {
        // Thanks to `ignoreTrailingSlash` it will serve both `/foo` and `/foo/`.
        complete("OK")
      } ~
        path("bar" /) {
          // Thanks to `ignoreTrailingSlash` it will serve both `/bar` and `/bar/`.
          complete("OK")
        }
    }

    // tests:
    Get("/foo") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }

    Get("/foo/") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }

    Get("/bar") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }

    Get("/bar/") ~> route ~> check {
      status shouldEqual StatusCodes.OK
      responseAs[String] shouldEqual "OK"
    }
    //#ignoreTrailingSlash
  }
}
