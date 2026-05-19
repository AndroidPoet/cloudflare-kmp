import * as kotlinWorker from "../build/compileSync/js/main/productionExecutable/kotlin/cloudflare-kmp-worker-template.mjs";

const fetchHandler = kotlinWorker.fetch;

export default {
  fetch(request, env) {
    return fetchHandler(request, env);
  },
};
