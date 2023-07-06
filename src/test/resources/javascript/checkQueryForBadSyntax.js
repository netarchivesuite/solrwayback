function checkQueryForBadSyntax(q) {

        q = q.replaceAll('\n', ' ');
      
        var responses = [];
        if (((q.includes(' AND ')  || q.includes(' && ')) &&
            (q.includes(' OR ') || q.includes(' || '))) && 
            !q.includes('(')) {
          responses.push('Ambiguous AND/OR. Consider adding clarifying parentheses');
        }
        
        if (q.match(/ (and|or|not) /)) {
          responses.push('Possible faulty boolean. Booleans must be uppercase - ' +  q.replace(' and ', ' AND ').replace(' or ', ' OR ').replace(' not ', ' NOT '));
        }

        if (q.match(/[`‘’''„“‟”❝❞]/)) {
          responses.push('Smart quotes. When quoting, use simple quote signs " - ' +  q.replace(/[`‘’'„“‟”❝❞]/ + '"'));
        }

        // Remove all 'f:[something TO somethingelse] to avoid colon warnings for crawl_date:[2023-12-99T12:34:56Z TO *]
        var qfold = q.replace(/[^\\]: *\[ *[^ ]+ * TO  *[^ ]+ *]/g, '');
        if (qfold.match(/[^:"]+:[^:" ]*[^\\]:[^" ]( ?.*)$/)) {
          responses.push('Two colons without quote signs. When a qualified search is performed, consider quoting the value - ' + q.replace(/([^:"]*:)([^:" ]*[^\\]:[^" ]*)( ?.*)$/, '$1"$2"$3'));
        }

        if (q.match(/^https?:\/\/[^ ]*$/)) {
          responses.push('Standalone URL. Consider using URL-search for URLs');
        }

        if (q.match(/(^|\s)[*]($|\s)/)) {
          responses.push('Slow single star searches. These searches are very slow, use *:* to match everything try ' + q.replace(/(^|\s)([*])($|\s)/, '$1*:*$3'));
        }

        //Old regex discontinued due to no support for 'look behind' in Safari and IE11
        //let quoteMatches = q.replace('\\\\', '').match(/(?<!\\)"/g)
        var quoteMatches = q.replace('\\\\', '').replace('\\"', '').match(/"/g);
        if (quoteMatches && quoteMatches.length % 2 != 0) {
          responses.push('Unbalanced quotes. Make sure to write both a start- and an end-quote "');
        }
        
        var parenBalance = 0
        // Remove escapes, then iterate the tokens quotes_string, left_paren and right_paren
        q.replace(/\\\\/g, '').replace(/\\["()]/g, '').replace(/("[^"]*")|\(|\)/g, function(value, index, array) {
          // We only care about the side effect (adjusting paren balance), so there's no return value
          parenBalance += ('(' == value ? 1 : (')' == value ? -1 : 0));
        })
        
        if (parenBalance < 0) {
          responses.push(-parenBalance + ' missing start parenthes' + (parenBalance == -1 ? 'is' : 'es') + '. Make sure to balance parentheses')
        } else if (parenBalance > 0) {
          responses.push(parenBalance + ' missing end parenthes' + (parenBalance == 1 ? 'is' : 'es') + '. Make sure to balance parentheses')
        }
        
        return responses
     }
