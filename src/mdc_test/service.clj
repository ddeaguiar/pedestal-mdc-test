(ns mdc-test.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.log :as log]
            [io.pedestal.interceptor :as i]
            [clojure.core.async :as async]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]))

(def about-page
  {:name ::about-page
   :enter
   (fn [ctx]
     (log/with-context {"name" "Daniel"}
       (log/info :msg "foo!" :mdc-context log/*mdc-context*)
       (log/with-context {"last" "De Aguiar"}
         (log/info :msg "from go block!" :mdc-context log/*mdc-context*)
         (assoc ctx :response (ring-resp/response (format "Clojure %s - served from %s"
                                                          (clojure-version)
                                                          (route/url-for ::about-page)))))))})

;; INFO  io.pedestal.http -  - {:msg "GET /about", :line 80}
;; INFO  io.pedestal.http.cors -  - {:msg "cors request processing", :origin "", :allowed true, :line 84}
;; INFO  mdc-test.service - {"name" "Daniel"} - {:msg "foo!", :mdc-context {"name" "Daniel"}, :line 15}
;; INFO  mdc-test.service - {"name" "Daniel", "last" "De Aguiar"} - {:msg "from go block!", :mdc-context {"name" "Daniel", "last" "De Aguiar"}, :line 17}
;; INFO  io.pedestal.http.cors - {} - {:msg "cors response processing", :cors-headers {"Access-Control-Allow-Origin" "", "Access-Control-Allow-Credentials" "true", "Access-Control-Expose-Headers" "Strict-Transport-Security, X-Frame-Options, X-Content-Type-Options, X-Xss-Protection, X-Download-Options, X-Permitted-Cross-Domain-Policies, Content-Security-Policy, Content-Type"}, :line 111}

(def about-page2
  {:name ::about-page2
   :enter
   (fn [ctx]
     (log/with-context {"name" "Daniel"}
       (log/info :msg "foo!" :mdc-context log/*mdc-context*)
       (async/go
         (log/with-context {"last" "De Aguiar"}
           (log/info :msg "from go block!" :mdc-context log/*mdc-context*)
           (assoc ctx :response (ring-resp/response (format "Clojure %s - served from %s"
                                                            (clojure-version)
                                                            (route/url-for ::about-page))))))))})

;; INFO  io.pedestal.http -  - {:msg "GET /about2", :line 80}
;; INFO  io.pedestal.http.cors -  - {:msg "cors request processing", :origin "", :allowed true, :line 84}
;; INFO  mdc-test.service - {"name" "Daniel"} - {:msg "foo!", :mdc-context {"name" "Daniel"}, :line 27}
;; INFO  mdc-test.service - {"name" "Daniel", "last" "De Aguiar"} - {:msg "from go block!", :mdc-context {"name" "Daniel", "last" "De Aguiar"}, :line 30}
;; INFO  io.pedestal.http.cors - {"name" "Daniel"} - {:msg "cors response processing", :cors-headers {"Access-Control-Allow-Origin" "", "Access-Control-Allow-Credentials" "true", "Access-Control-Expose-Headers" "Strict-Transport-Security, X-Frame-Options, X-Content-Type-Options, X-Xss-Protection, X-Download-Options, X-Permitted-Cross-Domain-Policies, Content-Security-Policy, Content-Type"}, :line 111}

(defn home-page
  [request]
  (ring-resp/response "Hello World!"))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors about-page)]
              ["/about2" :get (conj common-interceptors about-page2)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])


;; Consumed by mdc-test.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port 8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})
