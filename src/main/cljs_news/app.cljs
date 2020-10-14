(ns cljs-news.app
  (:require
   [reagent.core :as r]
   [reagent.dom :as rd]
   [kitchen-async.promise :as p]
   [cljs.pprint :refer [pprint]]))

(def news (r/atom []))
(def selected-article (r/atom nil))

(defn req-json->clj
  [body]
  (p/-> body
        (.json)
        (js->clj :keywordize-keys true)))

(defn fetch-news
  []
  (p/->> (js/fetch "/clojure-news.json")
         (req-json->clj)
         (:articles)
         (take 8)))

(defn articles-view
  [articles]
  [:nav.articles
   [:ul.articles-list
    (for [article articles]
     [:li.articles-list__item
      {:key (:publishedAt article)}
      [:button.articles-list__button
       {:on-click #(reset! selected-article article)}
       [:h2.articles-list__title (:title article)]
       [:cite.articles-list__author (:author article)]
       [:date.articles-list__date (:publishedAt article)]]])]])

(defn summary-view
  [article]
  [:article.article
   (if-not article
     [:p.no-summary "Click an article to view details"]
     [:div.article__content
      [:h1.article__title (:title article)]
      (if (:urlToImage article)
        [:div.article__hero
         {:style {:background-image (str "url(" (:urlToImage article) ")")}}])
      [:p.article__description (:content article)]
      [:footer.article__footer
       [:a {:href (:url article)
            :target "_blank"} (:url article)]]])])

(defn news-view
  []
  (let [articles @news
        article @selected-article]
    [:div.news-view
     [articles-view articles]
     [summary-view article]]))

(defn app
  []
  (if (= (count @news) 0)
    [:div.loading "Loading news..."]
    [news-view]))

(defn reset-async!
  [an-atom promise]
  (p/let [res promise]
    (pprint res)
    (reset! an-atom res)))

(defn ^:dev/after-load init
  []
  (reset-async! news (fetch-news))
  (rd/render [app] (js/document.getElementById "app") ))
