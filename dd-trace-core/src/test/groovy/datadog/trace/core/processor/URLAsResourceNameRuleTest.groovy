package datadog.trace.core.processor

import datadog.trace.bootstrap.instrumentation.api.Tags
import datadog.trace.core.ExclusiveSpan
import datadog.trace.core.SpanFactory
import datadog.trace.core.processor.rule.URLAsResourceNameRule
import datadog.trace.util.test.DDSpecification
import spock.lang.Subject

class URLAsResourceNameRuleTest extends DDSpecification {

  @Subject
  def decorator = new URLAsResourceNameRule()

  def "pulls path from url #input"() {
    when:
    def path = decorator.extractResourceNameFromURL(null, input)

    then:
    path == expected

    where:
    input                                                            | expected
    ""                                                               | "/"
    "/"                                                              | "/"
    "/?asdf"                                                         | "/"
    "/search"                                                        | "/search"
    "/search?"                                                       | "/search"
    "/search?id=100&private=true"                                    | "/search"
    "/search?id=100&private=true?"                                   | "/search"
    "/users/?/:name"                                                 | "/users/?/:name"
    "http://localhost"                                               | "/"
    "http://localhost/"                                              | "/"
    "http://localhost/?asdf"                                         | "/"
    "http://local.host:8080/search"                                  | "/search"
    "http://local.host:8080/users/:userId"                           | "/users/:userId"
    "https://localhost:443/search?"                                  | "/search"
    "http://local.host:80/search?id=100&private=true"                | "/search"
    "http://localhost:80/search?id=100&private=true?"                | "/search"
    "http://10.0.0.1/?asdf"                                          | "/"
    "http://127.0.0.1/?asdf"                                         | "/"
    "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html" | "/index.html"
    "http://[1080:0:0:0:8:800:200C:417A]/index.html"                 | "/index.html"
    "http://[3ffe:2a00:100:7031::1]"                                 | "/"
    "http://[1080::8:800:200C:417A]/foo"                             | "/foo"
    "http://[::192.9.5.5]/ipng"                                      | "/ipng"
    "http://[::FFFF:129.144.52.38]:80/index.html"                    | "/index.html"
    "http://[2010:836B:4179::836B:4179]"                             | "/"
    "file:/some-random-file%abc"                                     | "file:/some-random-file%abc"
    "file:/some-random-file%abc/user1234"                            | "file:/some-random-file%abc/?"
    "https://dhajkdha/user1234"                                      | "/?"
    "abc"                                                            | "abc"
    "   "                                                            | "/"
    "   /:userId"                                                    | "/:userId"
    "\t/90"                                                          | "/?"
    "\t/:userId"                                                     | "/:userId"
  }

  def "should replace all digits"() {
    when:
    def norm = decorator.extractResourceNameFromURL(null, input)

    then:
    norm == output

    where:
    input              | output
    "/1"               | "/?"
    "/9999"            | "/?"
    "/user/1"          | "/user/?"
    "/user/1/"         | "/user/?/"
    "/user/1/repo/50"  | "/user/?/repo/?"
    "/user/1/repo/50/" | "/user/?/repo/?/"
  }

  def "should replace segments with mixed-characters"() {
    when:
    def norm = decorator.extractResourceNameFromURL(null, input)

    then:
    norm == output

    where:
    input                                              | output
    "/a1/v2"                                           | "/?/?"
    "/v3/1a"                                           | "/v3/?"
    "/V01/v9/abc/-1"                                   | "/V01/v9/abc/?"
    "/ABC/av-1/b_2/c.3/d4d/v5f/v699/7"                 | "/ABC/?/?/?/?/?/?/?"
    "/user/asdf123/repository/01234567-9ABC-DEF0-1234" | "/user/?/repository/?"
  }

  def "should leave other segments alone"() {
    when:
    def norm = decorator.extractResourceNameFromURL(null, input)

    then:
    norm == input

    where:
    input      | _
    "/v0/"     | _
    "/v10/xyz" | _
    "/a-b"     | _
    "/a_b"     | _
    "/a.b"     | _
    "/a-b/a-b" | _
    "/a_b/a_b" | _
    "/a.b/a.b" | _
  }

  def "sets the resource name"() {
    setup:
    def span = SpanFactory.newSpanOf(0)
    span.context.resourceName = null
    meta.each {
      span.setTag(it.key, (String) it.value)
    }

    when:
    span.context().processExclusiveSpan(new ExclusiveSpan.Consumer() {
      @Override
      void accept(ExclusiveSpan exclusiveSpan) {
        decorator.processSpan(exclusiveSpan)
      }
    })

    then:
    span.resourceName.toString() == resourceName

    where:
    value                       | resourceName        | meta
    null                        | "fakeOperation"     | [:]
    " "                         | "/"                 | [:]
    "\t"                        | "/"                 | [:]
    "/path"                     | "/path"             | [:]
    "/ABC/a-1/b_2/c.3/d4d/5f/6" | "/ABC/?/?/?/?/?/?"  | [:]
    "/not-found"                | "404"               | [(Tags.HTTP_STATUS): "404"]
    "/with-method"              | "POST /with-method" | [(Tags.HTTP_METHOD): "Post"]

    ignore = meta.put(Tags.HTTP_URL, value)
  }
}
