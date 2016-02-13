/* 
 * polymap.org
 * Copyright (C) 2015, Falko Br�utigam. All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.polymap.model2.store.recordstore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import org.polymap.model2.Entity;
import org.polymap.model2.query.grammar.BooleanExpression;
import org.polymap.model2.query.grammar.CompositeCollectionQuantifier;
import org.polymap.model2.query.grammar.IdPredicate;
import org.polymap.model2.query.grammar.ManyAssociationQuantifier;
import org.polymap.model2.query.grammar.Quantifier;
import org.polymap.model2.query.grammar.TheAssociationQuantifier;
import org.polymap.model2.query.grammar.TheCompositeQuantifier;
import org.polymap.model2.runtime.ModelRuntimeException;
import org.polymap.model2.test.Timer;
import org.polymap.recordstore.IRecordFieldSelector;
import org.polymap.recordstore.IRecordState;
import org.polymap.recordstore.QueryExpression;
import org.polymap.recordstore.RecordQuery;
import org.polymap.recordstore.ResultSet;
import org.polymap.recordstore.SimpleQuery;
import org.polymap.recordstore.lucene.LuceneRecordState;

/**
 * Handles all {@link Quantifier} types. 
 * <p/>
 * <b>FIXME</b> The {@link #subQuery(Class, BooleanExpression)} does not reflect
 * uncommitted modifications of the UnitOfWork. However, if the Entity itself is
 * modified then Quantifier implementation evaluates against the modified
 * (associated) entities in-memory, which returnes a correct result. So, the issue
 * exists just if the associated entities are modified but source entity of the
 * association is unmodified.
 * 
 * @author <a href="http://www.polymap.de">Falko Bräutigam</a>
 */
class LuceneQuantifierHandler
        extends LuceneExpressionHandler<Quantifier> {

    private static Log log = LogFactory.getLog( LuceneQuantifierHandler.class );

    
    @Override
    public Query handle( Quantifier quantifier ) {
        // composite
        if (quantifier instanceof TheCompositeQuantifier) {
            String fieldname = simpleFieldname( quantifier.prop );
            log( "THE Composite", fieldname );
            return builder.processExpression( fieldname, quantifier.subExp(), resultType );
        }

        // composite collection
        else if (quantifier instanceof CompositeCollectionQuantifier) {
            int maxElements = maxElementsInCollection( prefixedFieldname( quantifier.prop ).get() );
            
            // processExpressions accumulates the traversedPrefix; 
            // we give it something like: moreAddressed[0]
            FieldnameBuilder fieldname = FieldnameBuilder.EMPTY.composite( simpleFieldname( quantifier.prop ) );

            log( quantifier.type + " of Composites", fieldname );
            BooleanQuery result = new BooleanQuery();
            for (int i=0; i<maxElements; i++) {
                String elmname = fieldname.arrayElement( i ).get();
                Query propQuery = builder.processExpression( elmname, quantifier.subExp(), resultType );
                result.add( propQuery, 
                        quantifier.type == Quantifier.Type.ANY ? BooleanClause.Occur.SHOULD : BooleanClause.Occur.MUST );
            }
            return result;
        }
        
        // association
        if (quantifier instanceof TheAssociationQuantifier) {
            Class<? extends Entity> assocEntityType = quantifier.prop.info().getType();
            Object[] assocIds = subQuery( assocEntityType, quantifier.subExp() );
            
            FieldnameBuilder fieldname = prefixedFieldname( quantifier.prop );
            log( "THE association: " + fieldname + " contains any of", assocIds );
            return idQuery( fieldname.get(), assocIds );
        }
        
        // many association
        if (quantifier instanceof ManyAssociationQuantifier) {
            assert quantifier.type == Quantifier.Type.ANY;
            
            Class<? extends Entity> assocEntityType = quantifier.prop.info().getType();
            Object[] assocIds = subQuery( assocEntityType, quantifier.subExp() );
            
            FieldnameBuilder fieldname = prefixedFieldname( quantifier.prop );
            int maxElements = maxElementsInCollection( fieldname.get() );
            
            log( quantifier.type + " of association: " + fieldname + " contains any of", assocIds );
            BooleanQuery result = new BooleanQuery();
            for (int i=0; i<maxElements; i++) {
                String elmname = fieldname.arrayElement( i ).get();
                result.add( idQuery( elmname, assocIds ), BooleanClause.Occur.SHOULD );
            }
            return result;
        }
        
        else {
            throw new ModelRuntimeException( "Quantifier type not supported: " + quantifier );
        }
    }

    
    protected int maxElementsInCollection( String fieldname ) {
        try {
            Timer timer = new Timer();
            
            String lengthFieldname = fieldname + "/__size__";
            
            RecordQuery query = new SimpleQuery()
                    .eq( RecordCompositeState.TYPE_KEY, resultType.getName() )
                    .sort( lengthFieldname, SimpleQuery.DESC, Integer.class )
                    .setMaxResults( 1 );
            
            ResultSet lengthResult = builder.store.find( query );
            Integer result = 0;
            if (lengthResult.count() > 0) {
                IRecordState biggest = lengthResult.get( 0 );
                result = biggest.get( lengthFieldname );
            }
            log.debug( "    LUCENE: maxLength: " + result + " (" + timer.elapsedTime() + "ms)" );
            return result != null ? result : 0;
        }
        catch (Exception e) {
            log.info( "", e );
            throw new RuntimeException( e );
        }
    }


    protected Object[] subQuery( Class<? extends Entity> entityType, BooleanExpression exp ) {
        // FIXME this sub-query does not reflect uncommitted modifications of the UnitOfWork.
        // see class comment

        String subLogIndent = builder.logIndent + "    SUB[" + entityType.getSimpleName() + "] ";
        
        // optimize IdPredicate
        if (exp instanceof IdPredicate) {
            // we know the ids already, no need to execute query
            // XXX check if entity acrually exists?
            return ((IdPredicate)exp).ids;
        }
        
        // execute sub expression
        else {
            RecordQuery recordQuery = new LuceneQueryBuilder( builder.store, subLogIndent ).createQuery( entityType, exp );

            // load just the ID field
            recordQuery.setFieldSelector( new IRecordFieldSelector() {
                public boolean test( String key ) {
                    return key.equals( LuceneRecordState.ID_FIELD );
                }
            });

            try {
                ResultSet rs = builder.store.find( recordQuery );
                Object[] result = new Object[ rs.count() ];
                int i = 0;
                for (IRecordState state : rs) {
                    result[i] = state.id();
                    assert result[i] != null;
                }
                return result;
            }
            catch (Exception e) {
                throw new ModelRuntimeException( "Exception during association sub-query.", e );
            }
        }
    }


    protected Query idQuery( String fieldname, Object[] ids ) {
        if (ids.length == 1) {
            return idQuery( fieldname, ids[0] );
        }
        else {
            BooleanQuery result = new BooleanQuery();
            for (Object id : ids) {
                result.add( idQuery( fieldname, id ), BooleanClause.Occur.SHOULD );
            }
            return result;
        }
    }


    protected org.apache.lucene.search.Query idQuery( String fieldname, Object id ) {
        return builder.valueCoders.searchQuery( 
                new QueryExpression.Equal( fieldname, id ) );        
    }
    
}


///**
//* Handle the contains predicate.
//* <p/>
//* Impl. note: This needs a patch in
//* org.qi4j.runtime.query.grammar.impl.PropertyReferenceImpl<T> to work with
//* Qi4j 1.0.
//*/
//protected Query processContainsPredicate( ContainsPredicate predicate, String resultType ) {
// final ValueCoders valueCoders = store.getValueCoders();
//
// PropertyReference property = predicate.propertyReference();
// final String baseFieldname = property2Fieldname( property ).toString();
// SingleValueExpression valueExpression = (SingleValueExpression)predicate.valueExpression();
//
// //
// int maxElements = 10;
// try {
//     Timer timer = new Timer();
//     String lengthFieldname = baseFieldname + "__length";
//     RecordQuery query = new SimpleQuery()
//             .eq( "type", resultType )
//             .sort( lengthFieldname, SimpleQuery.DESC, Integer.class )
//             .setMaxResults( 1 );
//     ResultSet lengthResult = store.find( query );
//     IRecordState biggest = lengthResult.get( 0 );
//     maxElements = biggest.get( lengthFieldname );
//     log.debug( "    LUCENE: maxLength query: result: " + maxElements + " (" + timer.elapsedTime() + "ms)" );
// }
// catch (Exception e) {
//     throw new RuntimeException( e );
// }
// 
// //
// BooleanQuery result = new BooleanQuery();
// for (int i=0; i<maxElements; i++) {
//     final BooleanQuery valueQuery = new BooleanQuery();
//
//     final ValueComposite value = (ValueComposite)valueExpression.value();
//     ValueModel valueModel = (ValueModel)ValueInstance.getValueInstance( value ).compositeModel();
//     List<PropertyType> actualTypes = valueModel.valueType().types();
//     //                    json.key( "_type" ).value( valueModel.valueType().type().name() );
//
//
//     // all properties of the value
//     final int index = i;
//     value.state().visitProperties( new StateVisitor() {
//         public void visitProperty( QualifiedName name, Object propValue ) {
//             if (propValue == null) {
//             }
//             else if (propValue.toString().equals( "-1" )) {
//                 // FIXME hack to signal that this non-optional(!) value is not to be considered
//                 log.warn( "Non-optional field ommitted: " + name.name() + ", value=" + propValue );
//             }
//             else {
//                 String fieldname = Joiner.on( "" ).join( 
//                         baseFieldname, "[", index, "]", 
//                         LuceneEntityState.SEPARATOR_PROP, name.name() );
//                 
//                 //Property<Object> fieldProp = value.state().getProperty( name );
//
////               // this might not be the semantics of contains predicate but it is useless
////               // if one cannot do a search without (instead of just a strict match)
//                 Query propQuery = propValue instanceof String
//                         && !StringUtils.containsNone( (String)propValue, "*?")
//                         ? valueCoders.searchQuery( new QueryExpression.Match( fieldname, propValue ) ) 
//                         : valueCoders.searchQuery( new QueryExpression.Equal( fieldname, propValue ) ); 
//
//                 valueQuery.add( propQuery, BooleanClause.Occur.MUST );
//             }
//         }
//     });
//
//     result.add( valueQuery, BooleanClause.Occur.SHOULD );
// }
// return result;
//}
