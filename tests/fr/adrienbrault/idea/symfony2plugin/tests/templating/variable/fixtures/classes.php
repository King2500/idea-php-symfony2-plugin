<?php

namespace Symfony\Component\Templating;

class PhpEngine
{
    public function render($name, array $parameters = array())
    {
        return '';
    }
}

namespace Symfony\Bundle\FrameworkBundle\Templating;

use Symfony\Component\Templating\PhpEngine as BasePhpEngine;

class PhpEngine extends BasePhpEngine
{
}

namespace Foo\Controller;

class FooController
{
    public function fooAction()
    {
        /** @var \Symfony\Bundle\FrameworkBundle\Templating\PhpEngine $foo */
        $foo->render('dummy.html.php', [
            'foo' => 'bar',
            'dt' => new \DateTime(),
        ]);
    }
}