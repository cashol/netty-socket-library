# netty-socket-library
A library providing reactive socket producer and consumer functionality. 

Some preconditions:
1. Define GRADLE_USER_HOME as environment variable, e.g. GRADLE_USER_HOME=$HOME/DEV/gradle-user-home.
  (Location of local gradle repository)
2. Provide repos.gradle in $HOME/DEV/gradle-user-home.
3. Provide <strong>init.gradle</strong> in $HOME/DEV/gradle-user-home.
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
4. Provide publish.gradle in $HOME/DEV/gradle-user-home.
