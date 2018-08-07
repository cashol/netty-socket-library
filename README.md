# netty-socket-library
A library providing reactive socket producer and consumer functionality.
Look at <a href="https://github.com/cashol/netty-socket-library/blob/master/src/test/java/com/siemens/ra/ts/nettysocketlibrary/test/NettySocketLibraryTest.java">NettySocketLibraryTest.java</a> for usage.

Some preconditions:
1. Define GRADLE_USER_HOME as environment variable, e.g. GRADLE_USER_HOME=$HOME/DEV/gradle-user-home.
  (Location of local gradle repository)
2. Provide <strong>repos.gradle</strong> in $HOME/DEV/gradle-user-home:
<pre><code>repositories {
  maven { 
    url uri(System.getenv("GRADLE_USER_HOME"))
  }
  mavenLocal()
  mavenCentral()
}
</code></pre>
3. Provide <strong>init.gradle</strong> in $HOME/DEV/gradle-user-home:
<pre><code>allprojects {
    ext {
      slf4jVersion = '1.8.0-beta2'
      apacheCommonCliVersion = '1.4'
      jaxbVersion = '2.3.0'
      javaxActivationVersion = '1.1.1'
      junitPlatformVersion = '1.2.0'
      junitJupiterVersion = '5.2.0'
      assertjVersion = '3.10.0'
      awaitilityVersion = '3.1.2'
      gradleUserHome = System.getenv("GRADLE_USER_HOME")
    }
    // Common repositories:
    apply from: "${gradleUserHome}/repos.gradle", to: allprojects
  }
</code></pre>
4. Provide <strong>publish.gradle</strong> in $HOME/DEV/gradle-user-home:
<pre><code>repositories {
  maven {
    // Set correct values for ip address, port and credentials
    url "http://10.10.10.10:8081/nexus/content/repositories/snapshots/"
      credentials {
        username 'user'
        password 'password'
      }
    }
  }
}
</code></pre>
