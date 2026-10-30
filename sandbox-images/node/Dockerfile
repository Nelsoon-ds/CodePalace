FROM node:22-alpine@sha256:fe4f6d74e20370acb04ad889077d14d0f64a9c8ea6a42bc60cddf1e844699914

RUN adduser -D -u 10001 kata

WORKDIR /sandbox

# vitest 5.0.5 (commit 7a825db43e2beca0b219d2eadaec334cb81af0c7)
RUN npm init -y >/dev/null \
    && npm install --save-exact vitest@5.0.5 \
    && mkdir -p /sandbox/tmp /sandbox/.vitest \
    && chown kata:kata /sandbox/tmp /sandbox/.vitest

COPY vitest.config.mjs /sandbox/vitest.config.mjs

USER kata

ENTRYPOINT ["/sandbox/node_modules/.bin/vitest", "run", "--configLoader=runner"]
