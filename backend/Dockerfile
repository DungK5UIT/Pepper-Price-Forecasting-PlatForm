# Backend image: Spring Boot on Java 21.
#
# Two stages so the JDK, the Maven cache and the sources stay out of what
# ships. The runtime layer carries a JRE and one jar.

# --- build ---------------------------------------------------------------
# Pinned to the Ubuntu 22.04 (jammy) variant rather than the floating
# `21-jdk` tag: that tag has since moved to a newer Ubuntu base whose
# binaries this platform's containerd/runc cannot exec (ENOEXEC), while
# jammy is unaffected.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /build

# Dependencies resolve from the pom alone, so this layer is rebuilt only when
# the pom changes — not on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests run in CI against a fixed JDK, not here: an image build that reaches
# for the network mid-build fails in ways that have nothing to do with the
# image. See .github/workflows once CI exists.
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# --- runtime -------------------------------------------------------------
# Same jammy pin as the build stage — see the note there.
FROM eclipse-temurin:21-jre-jammy AS runtime

# Log timestamps read as Vietnamese local time. The application does not rely
# on this — app.time-zone pins every date it stores — but a log line an hour
# out from the events it describes is its own kind of bug report.
ENV TZ=Asia/Ho_Chi_Minh

# The JVM sizes its heap from the container limit rather than the host's
# memory, which is otherwise how a 512 MB container gets an OOM kill.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# Unprivileged: this process scrapes two public websites and parses what they
# return, so it is the one part of the platform handling untrusted input.
RUN useradd --system --create-home --uid 10001 app
USER app
WORKDIR /home/app

COPY --from=build --chown=app:app /build/app.jar app.jar

EXPOSE 8080

# Deliberately no HEALTHCHECK: /actuator/health reports STALE when collection
# has gone quiet, which is a data problem, not a reason for a supervisor to
# restart a working process. See ADR-0005.

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
