(function () {
    const activeRequests = new Map();
    const suggestionCache = new Map();
    const formSubmitHandlers = new WeakSet();
    const minimumAutocompleteLength = 2;

    function clearSuggestions(listElement) {
        listElement.innerHTML = "";
    }

    function fillSuggestions(listElement, suggestions) {
        clearSuggestions(listElement);

        suggestions.forEach(function (name) {
            const option = document.createElement("option");
            option.value = name;
            listElement.appendChild(option);
        });
    }

    function resolveGameLookup(term) {
        return fetch("/api/boardgames/resolve?q=" + encodeURIComponent(term))
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Resolve request failed");
                }
                return response.json();
            })
            .then(function (data) {
                return data && typeof data.resolvedName === "string"
                    ? data
                    : { resolvedName: "", exactMatch: false };
            })
            .catch(function () {
                return { resolvedName: "", exactMatch: false };
            });
    }

    function bindResolveOnEnter(input, options) {
        const settings = options || {};
        const form = input.form;
        if (!form || form.dataset.boardgamePageSearch === "true" || formSubmitHandlers.has(form)) {
            return;
        }

        formSubmitHandlers.add(form);
        form.addEventListener("submit", function (event) {
            if (form.dataset.skipResolveSubmit === "true") {
                form.dataset.skipResolveSubmit = "false";
                return;
            }

            if (form.dataset.isResolvingSubmit === "true") {
                return;
            }

            const gameInputElements = Array.from(form.querySelectorAll("input[data-boardgame-input='true']"));
            if (gameInputElements.length === 0) {
                return;
            }

            event.preventDefault();
            form.dataset.isResolvingSubmit = "true";

            let shouldRedirectToSearch = false;
            let redirectQuery = "";

            Promise.all(
                gameInputElements.map(function (field) {
                    const term = field.value.trim();
                    if (term.length === 0) {
                        return Promise.resolve();
                    }

                    return resolveGameLookup(term).then(function (resolvedGame) {
                        if (resolvedGame.exactMatch && resolvedGame.resolvedName) {
                            field.value = resolvedGame.resolvedName;
                            return;
                        }

                        if (settings.redirectOnNoExactMatch === true) {
                            shouldRedirectToSearch = true;
                            redirectQuery = term;
                        }
                    });
                })
            ).finally(function () {
                form.dataset.isResolvingSubmit = "false";

                if (shouldRedirectToSearch) {
                    window.location.href = "/games/search?q=" + encodeURIComponent(redirectQuery);
                    return;
                }

                form.dataset.skipResolveSubmit = "true";
                form.requestSubmit();
            });
        });
    }

    function attachGameAutocomplete(inputId, listId) {
        const input = document.getElementById(inputId);
        const list = document.getElementById(listId);

        if (!input || !list) {
            return;
        }

        input.dataset.boardgameInput = "true";
        bindResolveOnEnter(input, { redirectOnNoExactMatch: inputId === "gameTitle" });

        let debounceTimer = null;

        input.addEventListener("input", function () {
            const term = input.value.trim();

            if (debounceTimer) {
                clearTimeout(debounceTimer);
            }

            debounceTimer = setTimeout(function () {
                if (term.length < minimumAutocompleteLength) {
                    clearSuggestions(list);
                    return;
                }

                const cacheKey = inputId + "::" + term.toLowerCase();
                if (suggestionCache.has(cacheKey)) {
                    fillSuggestions(list, suggestionCache.get(cacheKey));
                    return;
                }

                const previousRequest = activeRequests.get(inputId);
                if (previousRequest) {
                    previousRequest.abort();
                }

                const controller = new AbortController();
                activeRequests.set(inputId, controller);

                fetch("/api/boardgames/autocomplete?q=" + encodeURIComponent(term), {
                    signal: controller.signal
                })
                    .then(function (response) {
                        if (!response.ok) {
                            throw new Error("Autocomplete request failed");
                        }
                        return response.json();
                    })
                    .then(function (suggestions) {
                        if (Array.isArray(suggestions)) {
                            suggestionCache.set(cacheKey, suggestions);
                            fillSuggestions(list, suggestions);
                        } else {
                            clearSuggestions(list);
                        }
                    })
                    .catch(function (error) {
                        if (error.name !== "AbortError") {
                            clearSuggestions(list);
                        }
                    });
            }, 120);
        });

        input.addEventListener("keydown", function (event) {
            if (event.key !== "Enter") {
                return;
            }

            const form = input.form;
            if (!form) {
                return;
            }

            event.preventDefault();
            form.requestSubmit();
        });
    }

    function attachGamePageSearch(inputId, listId) {
        const input = document.getElementById(inputId);
        if (!input || !input.form) {
            return;
        }

        input.form.dataset.boardgamePageSearch = "true";

        if (listId) {
            attachGameAutocomplete(inputId, listId);
        }

        const form = input.form;
        if (form.dataset.boardgamePageSearchBound === "true") {
            return;
        }

        form.dataset.boardgamePageSearchBound = "true";
        form.addEventListener("submit", function (event) {
            event.preventDefault();

            const query = input.value.trim();
            if (!query) {
                return;
            }

            resolveGameLookup(query)
                .then(function (resolvedGame) {
                    if (resolvedGame.exactMatch && resolvedGame.resolvedName) {
                        window.location.href = "/games/" + encodeURIComponent(resolvedGame.resolvedName);
                        return;
                    }

                    window.location.href = "/games/search?q=" + encodeURIComponent(query);
                })
                .catch(function () {
                    window.location.href = "/games/search?q=" + encodeURIComponent(query);
                });
        });

        input.addEventListener("keydown", function (event) {
            if (event.key !== "Enter") {
                return;
            }

            event.preventDefault();
            form.requestSubmit();
        });
    }

    window.attachGameAutocomplete = attachGameAutocomplete;
    window.attachGamePageSearch = attachGamePageSearch;
})();
