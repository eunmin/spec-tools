(ns spec-tools.spec
  (:refer-clojure :exclude [any? some? number? pos? neg? integer? int? pos-int? neg-int? nat-int?
                            float? double? boolean? string? ident? simple-ident? qualified-ident?
                            keyword? simple-keyword? qualified-keyword? symbol? simple-symbol?
                            qualified-symbol? uuid? uri? decimal? inst? seqable? indexed?
                            map? vector? list? seq? char? set? nil? false? true? zero?
                            rational? coll? empty? associative? sequential? ratio? bytes?
                            merge
                            #?@(:cljs [Inst Keyword UUID])])
  (:require [clojure.spec.alpha :as s]
            [clojure.set :as set]
            [spec-tools.core :as st]
            [spec-tools.impl :as impl]
            [spec-tools.parse :as parse]))

(def any? (st/spec clojure.core/any?))
(def some? (st/spec clojure.core/some?))
(def number? (st/spec clojure.core/number?))
(def pos? (st/spec clojure.core/pos?))
(def neg? (st/spec clojure.core/neg?))
(def integer? (st/spec clojure.core/integer?))
(def int? (st/spec clojure.core/int?))
(def pos-int? (st/spec clojure.core/pos-int?))
(def neg-int? (st/spec clojure.core/neg-int?))
(def nat-int? (st/spec clojure.core/nat-int?))
(def float? (st/spec clojure.core/float?))
(def double? (st/spec clojure.core/double?))
(def boolean? (st/spec clojure.core/boolean?))
(def string? (st/spec clojure.core/string?))
(def ident? (st/spec clojure.core/ident?))
(def simple-ident? (st/spec clojure.core/simple-ident?))
(def qualified-ident? (st/spec clojure.core/qualified-ident?))
(def keyword? (st/spec clojure.core/keyword?))
(def simple-keyword? (st/spec clojure.core/simple-keyword?))
(def qualified-keyword? (st/spec clojure.core/qualified-keyword?))
(def symbol? (st/spec clojure.core/symbol?))
(def simple-symbol? (st/spec clojure.core/simple-symbol?))
(def qualified-symbol? (st/spec clojure.core/qualified-symbol?))
(def uuid? (st/spec clojure.core/uuid?))
#?(:clj (def uri? (st/spec clojure.core/uri?)))
#?(:clj (def decimal? (st/spec clojure.core/decimal?)))
(def inst? (st/spec clojure.core/inst?))
(def seqable? (st/spec clojure.core/seqable?))
(def indexed? (st/spec clojure.core/indexed?))
(def map? (st/spec clojure.core/map?))
(def vector? (st/spec clojure.core/vector?))
(def list? (st/spec clojure.core/list?))
(def seq? (st/spec clojure.core/seq?))
(def char? (st/spec clojure.core/char?))
(def set? (st/spec clojure.core/set?))
(def nil? (st/spec clojure.core/nil?))
(def false? (st/spec clojure.core/false?))
(def true? (st/spec clojure.core/true?))
(def zero? (st/spec clojure.core/zero?))
#?(:clj (def rational? (st/spec clojure.core/rational?)))
(def coll? (st/spec clojure.core/coll?))
(def empty? (st/spec clojure.core/empty?))
(def associative? (st/spec clojure.core/associative?))
(def sequential? (st/spec clojure.core/sequential?))
#?(:clj (def ratio? (st/spec clojure.core/ratio?)))
#?(:clj (def bytes? (st/spec clojure.core/bytes?)))

(defn- map-spec-keys [spec]
  (let [spec (or (if (qualified-keyword? spec)
                   (s/form spec))
                 spec)
        info (parse/parse-spec spec)]
    (select-keys info [:keys :keys/req :keys/opt])))

(defn merge-impl [forms spec-form merge-spec]
  (let [form-keys (map map-spec-keys forms)
        spec (reify
               s/Spec
               (conform* [_ x]
                 (let [conformed-vals (map #(s/conform % x) forms)]
                   (if (some #{::s/invalid} conformed-vals)
                     ::s/invalid
                     (apply clojure.core/merge x (map #(select-keys %1 %2) conformed-vals (map :keys form-keys))))))
               (unform* [_ x]
                 (s/unform* merge-spec x))
               (explain* [_ path via in x]
                 (s/explain* merge-spec path via in x))
               (gen* [_ overrides path rmap]
                 (s/gen* merge-spec overrides path rmap)))]
    (st/create-spec (clojure.core/merge {:spec spec
                                         :form spec-form
                                         :type :map}
                                        (apply merge-with set/union form-keys)))))

(defmacro merge [& forms]
  `(let [merge-spec# (s/merge ~@forms)]
     (merge-impl ~(vec forms) '(spec-tools.spec/merge ~@(map #(impl/resolve-form &env %) forms)) merge-spec#)))

;; spec-tools.spec/merge is normalized to clojure.core/merge
(defmethod parse/parse-form 'clojure.core/merge
  [_ form]
  (apply impl/deep-merge (map parse/parse-spec (rest form))))
