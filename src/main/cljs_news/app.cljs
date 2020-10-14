(ns cljs-news.app
  (:require
   [reagent.core :as r]
   [reagent.dom :as rd]
   [kitchen-async.promise :as p]
   [cljs.pprint :refer [pprint]]))

(def news (r/atom []))
(def selected-article (r/atom nil))

(defn reset-async!
  "
  Takes an atom and a promise.
  Resets the contents of the atom to the results of the promise on resolve.
  Returns a promise.
  "
  [an-atom promise]
  (p/let [res promise]
    (pprint res)
    (reset! an-atom res)))

(defn json->clj
  "
  Takes js or parsed JSON data.
  Returns a cljs hash-map with :keyword keys
  "
  [json-data]
  (js->clj json-data :keywordize-keys true))

(defn fetch-news
  "
  Fetches pre-baked JSON data from the local server
  Returns a promise resolving a list of the 8 latest articles from the JSON.
  "
  []
  (p/->> (js/fetch "/clojure-news.json")
         (.json)
         (json->clj)
         (:articles)
         (take 8)))

(defn articles-view
  "
  Sidebar displaying a list of articles. Clicking on title selects an article.
  Takes a list of article hash-maps to render
  Returns a nav element in hiccup form
  "
  [articles]
  [:nav.articles
   [:ul.articles-list
    (for [article articles]
      [:li.articles-list__item
       {:key (:publishedAt article)}
       [:button.articles-list__button
        {:on-click #(reset! selected-article article)}
        [:h2.articles-list__title (:title article)]
        [:date.articles-list__date (:publishedAt article)]]])]])

(defn summary-view
  "
  Displays details about the selected article. Displays a prompt if no article
  is selected.

  Takes an article hash-map
  Returns an article hiccup element with article summary content or prompt
  paragraph if no article is selected.
  "
  [article]
  [:article.article
   (if-not article
     [:p.no-summary "Click an article to view details"]
     [:div.article__content
      [:header.article__head
       [:h1.article__title (:title article)]
       [:p.article__byline
        [:date.article__date (:publishedAt article)]
        " by "
        [:cite.article__author (:author article)]]]
      (if (:urlToImage article)
        [:div.article__hero
         {:style {:background-image (str "url(" (:urlToImage article) ")")}}])
      [:p.article__description (:content article)]
      [:footer.article__footer
       [:a {:href (:url article)
            :target "_blank"} (:url article)]]])])

(defn news-view
  "
  Base view for our news UI. Reads the list of news articles and currently
  selected article from our stateful atoms.
  "
  []
  (let [articles @news
        article @selected-article]
    [:div.news-view
     [articles-view articles]
     [summary-view article]]))

(defn app
  "
  Root view.
  Displays loading news text until there's at least one article.
  "
  []
  (if (= (count @news) 0)
    [:div.loading "Loading news..."]
    [news-view]))


(defn ^:dev/after-load init
  "
  App entrypoint. Runs after loading updated code in development mode.
  Mounts the root app view element to a <div id=\"app\">...</div>
  "
  []
  (reset-async! news (fetch-news))
  (rd/render [app] (js/document.getElementById "app") ))
