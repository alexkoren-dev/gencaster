import uuid
from unittest import mock

from asgiref.sync import async_to_sync, sync_to_async
from django.test import TestCase

from story_graph.models import Edge, Graph, Node
from story_graph.tests import EdgeTestCase, GraphTestCase, NodeTestCase

from .schema import schema


class SchemaTestCase(TestCase):
    @staticmethod
    def get_login_context(is_authenticated=True):
        m = mock.MagicMock()
        m.request.user.is_authenticated = is_authenticated
        return m

    NODE_MUTATION = """
        mutation TestMutation($name:String!, $graphUuid:UUID!, $color:String) {
            addNode(newNode: {name: $name, graphUuid: $graphUuid, color: $color})
        }
    """

    @async_to_sync
    async def test_add_node(self):
        graph = await sync_to_async(GraphTestCase.get_graph)()

        resp = await schema.execute(
            self.NODE_MUTATION,
            variable_values={"name": "foo", "graphUuid": str(graph.uuid)},
            context_value=self.get_login_context(),
        )

        self.assertIsNone(resp.errors)
        self.assertEqual(await Node.objects.all().acount(), 1)

    @async_to_sync
    async def test_add_node_no_auth(self):
        graph = await sync_to_async(GraphTestCase.get_graph)()

        resp = await schema.execute(
            self.NODE_MUTATION,
            variable_values={"name": "foo", "graphUuid": str(graph.uuid)},
            context_value=self.get_login_context(is_authenticated=False),
        )

        self.assertGreaterEqual(len(resp.errors), 1)  # type: ignore
        self.assertEqual(await Node.objects.all().acount(), 0)

    @async_to_sync
    async def test_add_node_with_color(self):
        graph = await sync_to_async(GraphTestCase.get_graph)()

        resp = await schema.execute(
            self.NODE_MUTATION,
            variable_values={
                "name": "foo",
                "graphUuid": str(graph.uuid),
                "color": "#aaa",
            },
            context_value=self.get_login_context(),
        )
        node = await Node.objects.afirst()
        self.assertIsNotNone(node)
        self.assertEqual(node.color, "#aaa")  # type: ignore

    NODE_UPDATE_MUTATION = """
    mutation updateNode($nodeUuid: UUID!, $name: String, $color: String, $positionX: Float, $positionY: Float) {
        updateNode(
            nodeUpdate: {uuid: $nodeUuid, name: $name, color: $color, positionX: $positionX, positionY: $positionY}
        )
    }
    """

    @async_to_sync
    async def test_update_node(self):
        node: Node = await sync_to_async(NodeTestCase.get_node)()

        resp = await schema.execute(
            self.NODE_UPDATE_MUTATION,
            variable_values={
                "nodeUuid": str(node.uuid),
                "name": "foo",
                "positionX": 20.0,
                "positionY": 40.0,
                "color": "#aaa",
            },
            context_value=self.get_login_context(),
        )

        self.assertEqual(await Node.objects.all().acount(), 1)
        node_db: Node = await Node.objects.afirst()  # type: ignore
        self.assertIsNotNone(node_db)
        self.assertEqual(node_db.name, "foo")
        self.assertEqual(node_db.position_x, 20.0)
        self.assertEqual(node_db.position_y, 40.0)
        self.assertEqual(node_db.color, "#aaa")

    @async_to_sync
    async def test_add_edge(self):
        graph: Graph = await sync_to_async(GraphTestCase.get_graph)()
        in_node: Node = await sync_to_async(NodeTestCase.get_node)(graph=graph)
        out_node: Node = await sync_to_async(NodeTestCase.get_node)(graph=graph)

        mutation = """
            mutation TestMutation($nodeInUuid:UUID!, $nodeOutUuid:UUID!) {
                addEdge(newEdge: {nodeInUuid: $nodeInUuid, nodeOutUuid: $nodeOutUuid})
            }
        """

        resp = await schema.execute(
            mutation,
            variable_values={
                "nodeInUuid": str(in_node.uuid),
                "nodeOutUuid": str(out_node.uuid),
            },
            context_value=self.get_login_context(),
        )

        self.assertIsNone(resp.errors)

        self.assertEqual(await Edge.objects.all().acount(), 1)

        edge: Edge = await Edge.objects.all().afirst()  # type: ignore
        self.assertEqual(await sync_to_async(lambda: edge.in_node)(), in_node)
        self.assertEqual(await sync_to_async(lambda: edge.out_node)(), out_node)

    GRAPH_QUERY = """
        query TestQuery {
            graphs {
                name
            }
        }
    """

    @async_to_sync
    async def test_no_auth(self):
        resp = await schema.execute(
            self.GRAPH_QUERY,
            context_value=self.get_login_context(is_authenticated=False),
        )

        self.assertGreaterEqual(len(resp.errors), 1)  # type: ignore

    @async_to_sync
    async def test_with_auth(self):
        resp = await schema.execute(
            self.GRAPH_QUERY,
            context_value=self.get_login_context(is_authenticated=True),
        )
        self.assertIsNone(resp.errors)
        self.assertDictEqual(resp.data, {"graphs": []})  # type: ignore

    EDGE_DELETE_MUTATION = """
        mutation TestMutation($edgeUuid: UUID!) {
            deleteEdge(edgeUuid: $edgeUuid)
        }
    """

    @async_to_sync
    async def test_delete_edge(self):
        edge: Edge = await sync_to_async(EdgeTestCase.get_edge)()

        resp = await schema.execute(
            self.EDGE_DELETE_MUTATION,
            variable_values={"edgeUuid": str(edge.uuid)},
            context_value=self.get_login_context(),
        )

        self.assertIsNone(resp.errors)
        self.assertEqual(await Edge.objects.all().acount(), 0)

    @async_to_sync
    async def test_delete_unavailable_edge(self):
        resp = await schema.execute(
            self.EDGE_DELETE_MUTATION,
            variable_values={"edgeUuid": str(uuid.uuid4())},
            context_value=self.get_login_context(),
        )

        self.assertGreaterEqual(len(resp.errors), 1)  # type: ignore

    NODE_DELETE_MUTATION = """
        mutation deleteNode($nodeUuid: UUID!) {
            deleteNode(nodeUuid: $nodeUuid)
        }
    """

    @async_to_sync
    async def test_delete_node(self):
        node: Node = await sync_to_async(NodeTestCase.get_node)()

        resp = await schema.execute(
            self.NODE_DELETE_MUTATION,
            variable_values={
                "nodeUuid": str(node.uuid),
            },
            context_value=self.get_login_context(),
        )

        self.assertIsNone(resp.errors)
        self.assertEqual(await Node.objects.all().acount(), 0)

    @async_to_sync
    async def test_delete_unavailable_node(self):
        resp = await schema.execute(
            self.NODE_DELETE_MUTATION,
            variable_values={"nodeUuid": str(uuid.uuid4())},
            context_value=self.get_login_context(),
        )

        self.assertGreaterEqual(len(resp.errors), 1)  # type: ignore